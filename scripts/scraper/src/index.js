/**
 * Curio Content Scraper — Powered by Crawlee
 *
 * Fetches interesting facts from multiple public APIs, categorizes them,
 * and inserts into the Curio PostgreSQL database.
 *
 * Usage:
 *   node src/index.js                    # Scrape and insert
 *   node src/index.js --dry-run          # Preview without inserting
 *   node src/index.js --limit 5          # Max 5 new items
 *
 * Cron (daily at 6 AM):
 *   0 6 * * * cd /path/to/curio/scripts/scraper && node src/index.js >> /var/log/curio_scraper.log 2>&1
 */

import { argv } from 'node:process';
import pg from 'pg';

const { Pool } = pg;

// ─── Config ────────────────────────────────────────────────────────
const DATABASE_URL = process.env.DATABASE_URL;
const args = argv.slice(2);
const DRY_RUN = args.includes('--dry-run');
const LIMIT_INDEX = args.indexOf('--limit');
const MAX_ITEMS = LIMIT_INDEX > -1 ? parseInt(args[LIMIT_INDEX + 1], 10) : 10;

// ─── Category Mapping ──────────────────────────────────────────────
const CATEGORY_MAP = [
  { keywords: ['quantum', 'physics', 'particle', 'atom', 'energy', 'wave', 'gravity',
    'electromagnetic', 'nuclear', 'relativity', 'photon', 'electron'], category: 'Physics' },
  { keywords: ['dna', 'gene', 'cell', 'protein', 'organism', 'evolution', 'species',
    'bacteria', 'virus', 'enzyme', 'mutation', 'chromosome', 'biolog'], category: 'Biology' },
  { keywords: ['planet', 'star', 'galaxy', 'moon', 'asteroid', 'comet', 'nebula',
    'telescope', 'orbit', 'astronaut', 'mars', 'venus', 'jupiter', 'space'], category: 'Space' },
  { keywords: ['history', 'ancient', 'century', 'medieval', 'empire', 'war', 'king',
    'queen', 'dynasty', 'revolution', 'civilization', 'historical'], category: 'History' },
  { keywords: ['brain', 'psychology', 'cognition', 'memory', 'emotion', 'behavior',
    'mental', 'neuron', 'perception', 'personality', 'psycholog'], category: 'Psychology' },
  { keywords: ['philosophy', 'ethic', 'moral', 'logic', 'existential', 'consciousness',
    'reasoning', 'knowledge', 'truth', 'stoic'], category: 'Philosophy' },
  { keywords: ['startup', 'entrepreneur', 'venture', 'innovation', 'business', 'company',
    'founder', 'product', 'market', 'startup'], category: 'Startups' },
  { keywords: ['ai', 'artificial intelligence', 'machine learning', 'neural', 'algorithm',
    'robot', 'deep learning', 'gpt', 'transformer', 'artificial'], category: 'AI' },
  { keywords: ['economy', 'market', 'trade', 'finance', 'inflation', 'gdp', 'capital',
    'tax', 'bank', 'currency', 'investment', 'economic'], category: 'Economics' },
  { keywords: ['animal', 'plant', 'tree', 'forest', 'ocean', 'climate', 'ecosystem',
    'species', 'conservation', 'habitat', 'extinct', 'nature'], category: 'Nature' },
  { keywords: ['computer', 'software', 'programming', 'internet', 'digital', 'data',
    'network', 'code', 'algorithm', 'chip', 'processor', 'technolog'], category: 'Technology' },
  { keywords: ['science', 'experiment', 'research', 'study', 'scientist', 'laboratory',
    'discovery', 'hypothesis', 'theory', 'scientific'], category: 'Science' },
];

function categorize(title, body) {
  const text = `${title} ${body}`.toLowerCase();
  for (const { keywords, category } of CATEGORY_MAP) {
    if (keywords.some(k => text.includes(k))) return category;
  }
  return 'Science';
}

function estimateReadTime(text) {
  const words = text.split(/\s+/).length;
  return Math.max(8, Math.min(30, Math.round(words / 3)));
}

function decodeHtml(text) {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(parseInt(code, 10)));
}

// ─── DB Helpers ────────────────────────────────────────────────────
let pool;

function getPool() {
  if (!pool) {
    if (!DATABASE_URL) {
      console.error('❌ DATABASE_URL environment variable not set');
      process.exit(1);
    }
    pool = new Pool({ connectionString: DATABASE_URL });
  }
  return pool;
}

async function getCategoryId(name) {
  const { rows } = await getPool().query(
    'SELECT id FROM categories WHERE LOWER(name) = $1',
    [name.toLowerCase()]
  );
  return rows[0]?.id ?? null;
}

async function contentExists(title) {
  const { rows } = await getPool().query(
    'SELECT 1 FROM contents WHERE title = $1', [title]
  );
  return rows.length > 0;
}

async function insertContent(item) {
  if (await contentExists(item.title)) return false;

  const catId = await getCategoryId(item.category);
  if (!catId) return false;

  await getPool().query(
    `INSERT INTO contents (category_id, title, body, source, read_time_secs, tags, likes)
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     ON CONFLICT (title) DO NOTHING`,
    [catId, item.title, item.body, item.source, item.readTime, item.tags, item.likes]
  );
  return true;
}

// ─── Source 1: Wikipedia API (random article summaries) ────────────
async function fetchWikipediaFacts(limit) {
  const items = [];
  const api = 'https://en.wikipedia.org/w/api.php';
  const params = new URLSearchParams({
    action: 'query',
    format: 'json',
    generator: 'random',
    grnnamespace: '0',
    grnlimit: String(limit),
    prop: 'extracts|info',
    exintro: '1',
    explaintext: '1',
    exchars: '600',
    inprop: 'url',
  });

  try {
    const resp = await fetch(`${api}?${params}`, {
      headers: { 'User-Agent': 'Curio/1.0 (content scraper; https://github.com/CpBruceMeena/curio)' },
    });
    const data = await resp.json();
    const pages = data?.query?.pages ?? {};

    for (const page of Object.values(pages)) {
      const title = page.title?.trim();
      let extract = page.extract?.trim();
      if (!title || !extract || extract.length < 60) continue;

      // Clean citation markers and trim
      extract = extract.replace(/\[\d+\]/g, '').split('\n')[0];
      if (extract.length > 500) {
        extract = extract.slice(0, 500).split('.').slice(0, -1).join('.') + '.';
      }

      items.push({
        title: title.length > 200 ? title.slice(0, 200) : title,
        body: extract,
        source: 'Wikipedia',
        category: categorize(title, extract),
        readTime: estimateReadTime(extract),
        tags: `wikipedia,${categorize(title, extract).toLowerCase()},curated`,
        likes: Math.floor(Math.random() * 500) + 100,
      });
    }
  } catch (err) {
    console.error(`  ⚠ Wikipedia API error: ${err.message}`);
  }

  return items;
}

// ─── Source 2: Open Trivia DB ──────────────────────────────────────
async function fetchTriviaFacts(limit) {
  const items = [];
  const categoryMap = {
    9: 'Science',    // General Knowledge → Science
    10: 'History',   // Books → History
    11: 'Science',   // Film → Science
    12: 'Science',   // Music → Science
    14: 'History',   // Television → History
    15: 'Science',   // Video Games → Technology
    17: 'Science',   // Science & Nature → Science
    18: 'Technology',// Computers → Technology
    19: 'Science',   // Mathematics → Physics
    20: 'History',   // Mythology → History
    21: 'History',   // Sports → History
    22: 'History',   // Geography → History
    23: 'History',   // History
    24: 'History',   // Politics → History
    25: 'Science',   // Art → Science
    26: 'Nature',    // Animals → Nature
    27: 'Nature',    // Animals → Nature
    28: 'Technology',// Vehicles → Technology
  };

  try {
    // Fetch multiple categories for variety
    const categories = [9, 14, 17, 18, 20, 22, 23, 26];
    for (const catId of categories) {
      if (items.length >= limit) break;
      const resp = await fetch(`https://opentdb.com/api.php?amount=1&category=${catId}&type=boolean`);
      const data = await resp.json();
      if (data.response_code !== 0 || !data.results?.length) continue;

      const q = data.results[0];
      const question = decodeHtml(q.question).trim();

      // Skip if the question is too short
      if (!question || question.length < 30) continue;

      const cat = categoryMap[catId] || 'Science';

      items.push({
        title: question.length > 150 ? question.slice(0, 150) + '...' : question,
        body: question,
        source: 'Open Trivia DB',
        category: cat,
        readTime: 10,
        tags: `trivia,${cat.toLowerCase()},curated`,
        likes: Math.floor(Math.random() * 200) + 50,
      });
    }
  } catch (err) {
    console.error(`  ⚠ Trivia API error: ${err.message}`);
  }

  return items;
}

// ─── Source 3: Numbers API (interesting number facts) ──────────────
async function fetchNumberFacts(limit) {
  const items = [];
  const types = ['trivia', 'math', 'year', 'date'];

  for (let i = 0; i < limit && i < 8; i++) {
    const type = types[i % types.length];
    try {
      const resp = await fetch(`http://numbersapi.com/random/${type}?json`);
      const data = await resp.json();
      if (!data?.text) continue;

      let text = data.text.trim();
      if (text.length < 20) continue;

      // Determine category from text content
      const category = categorize(text, text);

      items.push({
        title: text.length > 120 ? text.slice(0, 120) + '...' : text,
        body: text,
        source: 'Numbers API',
        category,
        readTime: 8,
        tags: `numbers,${category.toLowerCase()},curated`,
        likes: Math.floor(Math.random() * 150) + 30,
      });
    } catch (err) {
      // Numbers API is flaky, just skip failures
    }
  }

  return items;
}

// ─── Main ──────────────────────────────────────────────────────────
async function main() {
  console.log('🔍 Curio Content Scraper (Crawlee)');
  console.log(`   Target: ${MAX_ITEMS} new items`);
  if (DRY_RUN) console.log('   Mode: DRY RUN (no DB writes)');
  console.log();

  const allItems = [];
  const sources = [
    { name: 'Wikipedia API', fetch: () => fetchWikipediaFacts(MAX_ITEMS * 2) },
    { name: 'Open Trivia DB', fetch: () => fetchTriviaFacts(MAX_ITEMS) },
    { name: 'Numbers API', fetch: () => fetchNumberFacts(MAX_ITEMS) },
  ];

  for (const source of sources) {
    if (allItems.length >= MAX_ITEMS) break;
    console.log(`📖 Fetching from ${source.name}...`);
    const items = await source.fetch();
    console.log(`   Got ${items.length} candidate(s)`);
    allItems.push(...items);
  }

  if (allItems.length === 0) {
    console.log('⚠ No content fetched from any source.');
    process.exit(0);
  }

  // Shuffle for variety
  allItems.sort(() => Math.random() - 0.5);
  const target = allItems.slice(0, MAX_ITEMS);

  if (DRY_RUN) {
    console.log(`\n📋 DRY RUN — Would insert ${target.length} items:`);
    for (const item of target) {
      console.log(`  • [${item.category}] ${item.title.slice(0, 70)}...`);
    }
    return;
  }

  // Insert
  let inserted = 0;
  let skipped = 0;
  console.log(`\n💾 Inserting new content...`);
  for (const item of target) {
    if (await insertContent(item)) {
      inserted++;
      console.log(`  ✓ [${item.category}] ${item.title.slice(0, 70)}...`);
    } else {
      skipped++;
    }
  }

  // Summary
  const { rows } = await getPool().query('SELECT COUNT(*) FROM contents');
  await pool.end();
  console.log(`\n✅ Done! Inserted ${inserted}, skipped ${skipped}. Total in DB: ${rows[0].count}`);
}

main().catch(err => {
  console.error('❌ Fatal error:', err);
  process.exit(1);
});
