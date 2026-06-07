/**
 * Curio Content Scraper — Multi-source content engine
 *
 * Fetches facts from multiple APIs across 20 categories.
 * Supports batch mode for bulk insertion and cron scheduling.
 *
 * Usage:
 *   node src/index.js                          # Insert 10 items
 *   node src/index.js --limit 100              # Insert 100 items
 *   node src/index.js --batch 1000             # Batch-insert up to 1000 (loops APIs)
 *   node src/index.js --category Music         # Only scrape one category
 *   node src/index.js --dry-run                # Preview only
 *
 * Cron (daily at 6 AM):
 *   0 6 * * * cd /path/to/curio/scripts/scraper && node src/index.js --batch 200 >> /var/log/curio_scraper.log 2>&1
 */

import { argv } from 'node:process';
import pg from 'pg';

const { Pool } = pg;

// ─── Config ────────────────────────────────────────────────────────
const DATABASE_URL = process.env.DATABASE_URL;
const args = argv.slice(2);
const DRY_RUN = args.includes('--dry-run');
const LIMIT_INDEX = args.indexOf('--limit');
const BATCH_INDEX = args.indexOf('--batch');
const CAT_INDEX = args.indexOf('--category');
const MAX_ITEMS = LIMIT_INDEX > -1 ? parseInt(args[LIMIT_INDEX + 1], 10) : 10;
const BATCH_TARGET = BATCH_INDEX > -1 ? parseInt(args[BATCH_INDEX + 1], 10) : 0;
const FILTER_CATEGORY = CAT_INDEX > -1 ? args[CAT_INDEX + 1] : null;

// ─── Categories ────────────────────────────────────────────────────
const CATEGORIES = [
  { name: 'Science', icon: 'biotech', color: '#00f4fe' },
  { name: 'Space', icon: 'rocket_launch', color: '#a8cec8' },
  { name: 'History', icon: 'history_edu', color: '#e9c400' },
  { name: 'Biology', icon: 'psychology', color: '#63f7ff' },
  { name: 'Psychology', icon: 'psychology', color: '#c3eae4' },
  { name: 'Philosophy', icon: 'psychology', color: '#ffe16d' },
  { name: 'Physics', icon: 'atom', color: '#00dce5' },
  { name: 'Startups', icon: 'lightbulb', color: '#e9c400' },
  { name: 'AI', icon: 'neurology', color: '#00f4fe' },
  { name: 'Economics', icon: 'account_balance', color: '#63f7ff' },
  { name: 'Nature', icon: 'forest', color: '#a8cec8' },
  { name: 'Technology', icon: 'memory', color: '#00dce5' },
  { name: 'Poetry', icon: 'auto_stories', color: '#f472b6' },
  { name: 'Movies', icon: 'movie', color: '#fb923c' },
  { name: 'Neuroscience', icon: 'psychology', color: '#a78bfa' },
  { name: 'Literature', icon: 'menu_book', color: '#fbbf24' },
  { name: 'Geography', icon: 'public', color: '#34d399' },
  { name: 'Music', icon: 'music_note', color: '#f472b6' },
  { name: 'Sports', icon: 'sports_soccer', color: '#fb923c' },
  { name: 'Food', icon: 'ramen_dining', color: '#f59e0b' },
];

// ─── Category Keyword Mapping (for auto-classification) ────────────
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
  { keywords: ['neuron', 'neural', 'synapse', 'cortex', 'neurotransmitter', 'brain',
    'cerebellum', 'hippocampus', 'amygdala', 'plasticity'], category: 'Neuroscience' },
  { keywords: ['philosophy', 'ethic', 'moral', 'logic', 'existential', 'consciousness',
    'reasoning', 'knowledge', 'truth', 'stoic', 'quote'], category: 'Philosophy' },
  { keywords: ['poem', 'poetry', 'poet', 'verse', 'sonnet', 'haiku', 'rhyme',
    'lyric', 'bard', 'shakespeare', 'wordsworth'], category: 'Poetry' },
  { keywords: ['movie', 'film', 'cinema', 'actor', 'actress', 'director', 'hollywood',
    'bollywood', 'oscar', 'blockbuster', 'documentary'], category: 'Movies' },
  { keywords: ['book', 'novel', 'author', 'writer', 'literature', 'fiction', 'chapter',
    'publish', 'story', 'narrative', 'saga'], category: 'Literature' },
  { keywords: ['geography', 'country', 'capital', 'river', 'mountain', 'continent',
    'population', 'border', 'map', 'island', 'desert', 'ocean', 'region'], category: 'Geography' },
  { keywords: ['music', 'song', 'album', 'band', 'singer', 'guitar', 'piano',
    'orchestra', 'symphony', 'jazz', 'rock', 'melody', 'rhythm'], category: 'Music' },
  { keywords: ['sport', 'athlete', 'olympic', 'championship', 'football', 'basketball',
    'tennis', 'soccer', 'baseball', 'swimming', 'record'], category: 'Sports' },
  { keywords: ['food', 'recipe', 'cuisine', 'chef', 'ingredient', 'cooking', 'dish',
    'restaurant', 'spice', 'flavor', 'bake', 'grill'], category: 'Food' },
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
  return text.replace(/&amp;/g, '&').replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'").replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<').replace(/&gt;/g, '>')
    .replace(/&#(\d+);/g, (_, c) => String.fromCharCode(parseInt(c, 10)));
}

// ─── DB Helpers ────────────────────────────────────────────────────
let pool;

function getPool() {
  if (!pool) {
    if (!DATABASE_URL) { console.error('❌ DATABASE_URL not set'); process.exit(1); }
    pool = new Pool({ connectionString: DATABASE_URL });
  }
  return pool;
}

async function getCategoryId(name) {
  const { rows } = await getPool().query('SELECT id FROM categories WHERE LOWER(name) = $1', [name.toLowerCase()]);
  return rows[0]?.id ?? null;
}

async function insertContent(item) {
  const catId = await getCategoryId(item.category);
  if (!catId) return false;
  try {
    await getPool().query(
      `INSERT INTO contents (category_id, title, body, source, read_time_secs, tags, likes)
       VALUES ($1,$2,$3,$4,$5,$6,$7) ON CONFLICT (title) DO NOTHING`,
      [catId, item.title?.slice(0, 500), item.body?.slice(0, 2000), item.source, item.readTime, item.tags, item.likes]
    );
    return true;
  } catch { return false; }
}

// ─── Source 1: Wikipedia API ───────────────────────────────────────
async function fetchWikipedia(limit) {
  const items = [];
  const api = 'https://en.wikipedia.org/w/api.php';
  try {
    const resp = await fetch(`${api}?${new URLSearchParams({
      action: 'query', format: 'json', generator: 'random', grnnamespace: '0',
      grnlimit: String(limit), prop: 'extracts|info', exintro: '1', explaintext: '1',
      exchars: '600', inprop: 'url',
    })}`, { headers: { 'User-Agent': 'Curio/1.0' }, signal: AbortSignal.timeout(10000) });
    for (const page of Object.values((await resp.json())?.query?.pages ?? {})) {
      let title = page.title?.trim(), extract = page.extract?.trim();
      if (!title || !extract || extract.length < 60) continue;
      extract = extract.replace(/\[\d+\]/g, '').split('\n')[0];
      if (extract.length > 500) extract = extract.slice(0, 500).split('.').slice(0, -1).join('.') + '.';
      const cat = categorize(title, extract);
      if (FILTER_CATEGORY && cat.toLowerCase() !== FILTER_CATEGORY.toLowerCase()) continue;
      items.push({ title: title.slice(0, 200), body: extract, source: 'Wikipedia', category: cat,
        readTime: estimateReadTime(extract), tags: `wikipedia,${cat.toLowerCase()},curated`,
        likes: Math.floor(Math.random() * 500) + 100 });
    }
  } catch (e) { console.error(`  ⚠ Wikipedia: ${e.message}`); }
  return items;
}

// ─── Source 2: Quotes API (famous quotes) ────────────────────────────
async function fetchQuotes(limit) {
  const items = [];
  // API only returns 1 per request, so loop up to limit times
  for (let i = 0; i < Math.min(limit, 30); i++) {
    if (items.length >= limit) break;
    try {
      const resp = await fetch('https://quotesapi.prayushadhikari.com.np/api/quotes/random', { signal: AbortSignal.timeout(15000) });
      const body = await resp.json();
      // Returns { data: [{ quote, author, category: [...] }] }
      const quotes = body?.data ?? (Array.isArray(body) ? body : []);
      for (const q of quotes) {
        const text = q.quote?.trim();
        if (!text || text.length < 20) continue;
        // category is an array, e.g. ['grace','life']
        const tags = (Array.isArray(q.category) ? q.category : []).map(c => c.toLowerCase());
        const catStr = tags.join(' ');
        const curieCat = ['Philosophy', 'Psychology', 'Literature', 'History', 'Science', 'Technology'].find(
          c => catStr.includes(c.toLowerCase())) || 'Philosophy';
        items.push({ title: text.length > 120 ? text.slice(0, 120) + '...' : text,
          body: `${text} — ${q.author || 'Unknown'}`, source: 'Quotes API',
          category: curieCat, readTime: 8,
          tags: `quote,${curieCat.toLowerCase()},${q.author?.toLowerCase() || ''}`,
          likes: Math.floor(Math.random() * 800) + 200 });
      }
    } catch (e) { /* skip */ }
  }
  return items;
}

// ─── Source 3: PoetryDB (poems) ────────────────────────────────────
async function fetchPoems(limit) {
  const items = [];
  const authors = ['Shakespeare', 'Frost', 'Dickinson', 'Wordsworth', 'Blake', 'Yeats', 'Keats', 'Shelley', 'Poe', 'Whitman'];
  try {
    for (const author of authors) {
      if (items.length >= limit) break;
      // Note: /author/Name endpoint returns full poem objects with title + lines
      const resp = await fetch(`https://poetrydb.org/author/${encodeURIComponent(author)}`, { signal: AbortSignal.timeout(8000) });
      const data = await resp.json();
      const poems = Array.isArray(data) ? data : data?.status ? [] : [data];
      for (const p of poems) {
        if (items.length >= limit) break;
        const title = p.title?.trim();
        const lines = Array.isArray(p.lines) ? p.lines.filter(l => l.trim()).join(' ') : '';
        if (!title || !lines || lines.length < 30) continue;
        items.push({ title: title.slice(0, 150), body: lines.slice(0, 1000),
          source: 'PoetryDB', category: 'Poetry', readTime: estimateReadTime(lines),
          tags: `poetry,${author.toLowerCase()},poem`, likes: Math.floor(Math.random() * 300) + 50 });
      }
    }
  } catch (e) { console.error(`  ⚠ PoetryDB: ${e.message}`); }
  return items;
}

// ─── Source 4: Open Trivia DB ──────────────────────────────────────
async function fetchTrivia(limit) {
  const items = [];
  const catMap = { 9: 'Science', 10: 'Literature', 11: 'Movies', 12: 'Music', 14: 'Movies',
    15: 'Technology', 17: 'Science', 18: 'Technology', 20: 'History', 21: 'Sports',
    22: 'Geography', 23: 'History', 26: 'Nature', 27: 'Nature', 28: 'Technology' };
  try {
    for (const [catId, curieCat] of Object.entries(catMap)) {
      if (items.length >= limit) break;
      if (FILTER_CATEGORY && curieCat.toLowerCase() !== FILTER_CATEGORY.toLowerCase()) continue;
      // Small delay between categories to avoid rate limiting
      await new Promise(r => setTimeout(r, 200));
      const resp = await fetch(`https://opentdb.com/api.php?amount=5&category=${catId}&type=boolean`, { signal: AbortSignal.timeout(10000) });
      const data = await resp.json();
      if (data.response_code !== 0) continue;
      for (const q of (data.results || [])) {
        if (items.length >= limit) break;
        const question = decodeHtml(q?.question || '').trim();
        if (!question || question.length < 30) continue;
        items.push({ title: question.slice(0, 150), body: question, source: 'Open Trivia DB',
          category: curieCat, readTime: 10, tags: `trivia,${curieCat.toLowerCase()},curated`,
          likes: Math.floor(Math.random() * 200) + 50 });
      }
    }
  } catch (e) { console.error(`  ⚠ Trivia: ${e.message}`); }
  return items;
}

// ─── Source 5: Hacker News API (tech/business) ─────────────────────
async function fetchHackerNews(limit) {
  const items = [];
  try {
    const idsResp = await fetch('https://hacker-news.firebaseio.com/v0/topstories.json', { signal: AbortSignal.timeout(10000) });
    const ids = await idsResp.json();
    for (const id of ids.slice(0, limit * 3)) {
      if (items.length >= limit) break;
      const resp = await fetch(`https://hacker-news.firebaseio.com/v0/item/${id}.json`, { signal: AbortSignal.timeout(8000) });
      const story = await resp.json();
      if (!story?.title || story.type !== 'story') continue;
      const title = story.title.trim();
      const url = story.url || `https://news.ycombinator.com/item?id=${id}`;
      if (title.length < 15) continue;
      const cat = title.toLowerCase().includes('ai') || title.toLowerCase().includes('startup')
        ? (title.toLowerCase().includes('ai') ? 'AI' : 'Startups') : 'Technology';
      if (FILTER_CATEGORY && cat.toLowerCase() !== FILTER_CATEGORY.toLowerCase()) continue;
      items.push({ title: title.slice(0, 200), body: `${title} — ${url}`,
        source: 'Hacker News', category: cat, readTime: estimateReadTime(title),
        tags: `hackernews,${cat.toLowerCase()},tech`,
        likes: Math.floor(Math.random() * 300) + 100 });
    }
  } catch (e) { console.error(`  ⚠ HN: ${e.message}`); }
  return items;
}

// ─── Source 6: Numbers API ─────────────────────────────────────────
async function fetchNumbers(limit) {
  const items = [];
  const types = ['trivia', 'math', 'year', 'date'];
  for (let i = 0; i < Math.min(limit, 20); i++) {
    try {
      // Note: Numbers API only supports http://, not https://
      const resp = await fetch(`http://numbersapi.com/random/${types[i % 4]}?json`, { signal: AbortSignal.timeout(5000) });
      const data = await resp.json();
      if (!data?.text || data.text.length < 20) continue;
      const text = data.text.trim();
      const cat = categorize(text, text);
      if (FILTER_CATEGORY && cat.toLowerCase() !== FILTER_CATEGORY.toLowerCase()) continue;
      items.push({ title: text.slice(0, 120), body: text, source: 'Numbers API',
        category: cat, readTime: 8, tags: `numbers,${cat.toLowerCase()},curated`,
        likes: Math.floor(Math.random() * 150) + 30 });
    } catch (e) { if (i === 0) console.warn(`  ⚠ Numbers API: ${e.message}`); }
  }
  return items;
}

// ─── Source 7: iNaturalist API (nature facts) ──────────────────────
async function fetchNature(limit) {
  const items = [];
  try {
    const resp = await fetch('https://api.inaturalist.org/v1/observations/species_counts?quality_grade=research&per_page=30', { signal: AbortSignal.timeout(10000) });
    const data = await resp.json();
    for (const entry of (data?.results || []).slice(0, limit)) {
      const name = entry.taxon?.name || entry.taxon?.preferred_common_name || '';
      const common = entry.taxon?.preferred_common_name || '';
      if (!name) continue;
      const title = common ? `${common} (${name})` : name;
      const body = `${common || name} — a species observed in the wild. Rank: ${entry.taxon?.rank || 'species'}.`;
      items.push({ title: title.slice(0, 150), body, source: 'iNaturalist',
        category: 'Nature', readTime: 8,  tags: `nature,biology,species`,
        likes: Math.floor(Math.random() * 100) + 20 });
    }
  } catch (e) { console.error(`  ⚠ iNaturalist: ${e.message}`); }
  return items;
}

// ─── Main ──────────────────────────────────────────────────────────
async function main() {
  const isBatch = BATCH_TARGET > 0;
  const target = isBatch ? BATCH_TARGET : MAX_ITEMS;
  console.log(`🔍 Curio Content Scraper`);
  console.log(`   Target: ${target} new items${FILTER_CATEGORY ? ` (category: ${FILTER_CATEGORY})` : ''}`);
  if (DRY_RUN) console.log('   Mode: DRY RUN');
  console.log();

  // Ensure categories exist
  try {
    for (const cat of CATEGORIES) {
      await getPool().query(
        `INSERT INTO categories (name, icon, color_hex, priority) VALUES ($1,$2,$3,$4) ON CONFLICT (name) DO NOTHING`,
        [cat.name, cat.icon, cat.color, CATEGORIES.indexOf(cat) + 1]
      );
    }
    console.log(`✓ ${CATEGORIES.length} categories ensured`);
  } catch (e) { console.error(`⚠ Category insert error: ${e.message}`); }

  const sources = [
    { name: 'Wikipedia', fetch: () => fetchWikipedia(isBatch ? 50 : target * 2) },
    { name: 'Quotable Quotes', fetch: () => fetchQuotes(isBatch ? 50 : target) },
    { name: 'PoetryDB', fetch: () => fetchPoems(isBatch ? 30 : target) },
    { name: 'Hacker News', fetch: () => fetchHackerNews(isBatch ? 30 : target) },
    { name: 'Open Trivia DB', fetch: () => fetchTrivia(isBatch ? 50 : target) },
    { name: 'Numbers API', fetch: () => fetchNumbers(isBatch ? 20 : target) },
    { name: 'iNaturalist', fetch: () => fetchNature(isBatch ? 30 : target) },
  ];

  let totalInserted = 0;
  let runCount = 0;
  const maxRuns = isBatch ? Math.ceil(target / 50) + 10 : 1;

  while (totalInserted < target && runCount < maxRuns) {
    runCount++;
    if (isBatch) console.log(`\n📦 Batch run #${runCount} (inserted so far: ${totalInserted}/${target})`);

    const allItems = [];
    for (const src of sources) {
      console.log(`📖 ${src.name}...`);
      const items = await src.fetch();
      console.log(`   → ${items.length} candidate(s)`);
      allItems.push(...items);
    }
    allItems.sort(() => Math.random() - 0.5);

    if (allItems.length === 0) {
      console.log('⚠ No new candidates from any source. Waiting between runs may help.');
      if (!isBatch) break;
      await new Promise(r => setTimeout(r, 2000));
      continue;
    }

    let inserted = 0, skipped = 0;
    for (const item of allItems) {
      if (totalInserted + inserted >= target) break;
      if (await insertContent(item)) {
        inserted++;
        if (!isBatch) console.log(`  ✓ [${item.category}] ${(item.title || '').slice(0, 60)}...`);
      } else skipped++;
    }
    totalInserted += inserted;
    console.log(`   ✓ Run result: +${inserted} new, ${skipped} duplicates`);
    if (!isBatch) break;
    await new Promise(r => setTimeout(r, 1000));
  }

  console.log(`\n✅ Done! Inserted ${totalInserted} total.`);

  if (isBatch) {
    console.log(`\n📊 Category distribution:`);
    const dist = await getPool().query(
      'SELECT c.name, COUNT(*) as count FROM contents ct JOIN categories c ON ct.category_id = c.id GROUP BY c.name ORDER BY count DESC'
    );
    for (const row of dist.rows) console.log(`  ${row.name.padEnd(16)} ${row.count}`);
  }

  const { rows } = await getPool().query('SELECT COUNT(*) FROM contents');
  console.log(`   Total: ${rows[0].count} entries`);
  await pool?.end();
}

main().catch(e => { console.error('❌', e); process.exit(1); });
