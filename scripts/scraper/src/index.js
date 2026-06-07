/**
 * Curio Content Scraper — Powered by Crawlee
 *
 * Fetches interesting facts from multiple sources using Crawlee's CheerioCrawler,
 * categorizes them, and inserts into the Curio PostgreSQL database.
 *
 * Usage:
 *   node src/index.js              # Scrape and insert
 *   node src/index.js --dry-run    # Preview without inserting
 *   node src/index.js --limit 10   # Max 10 items
 *
 * Cron (daily at 6 AM):
 *   0 6 * * * cd /path/to/curio/scripts/scraper && node src/index.js >> /var/log/curio_scraper.log 2>&1
 */
import { CheerioCrawler, Dataset } from 'crawlee';
import pg from 'pg';
import { argv } from 'node:process';

const { Pool } = pg;

// ─── Config ────────────────────────────────────────────────────────
const DATABASE_URL = process.env.DATABASE_URL;
const args = argv.slice(2);
const DRY_RUN = args.includes('--dry-run');
const LIMIT_INDEX = args.indexOf('--limit');
const MAX_ITEMS = LIMIT_INDEX > -1 ? parseInt(args[LIMIT_INDEX + 1], 10) : 10;

// ─── Sources ───────────────────────────────────────────────────────
const SOURCES = [
  // Source 1: Wikipedia featured articles (random, well-written)
  {
    name: 'Wikipedia',
    url: 'https://en.wikipedia.org/wiki/Special:Random',
    handler: 'wikipedia',
  },
  // Source 2: wikidata.org interesting facts
  {
    name: 'Wikidata',
    url: 'https://www.wikidata.org/wiki/Special:Random',
    handler: 'wikidata',
  },
  // Source 3: Britannica "Did You Know?"
  {
    name: 'Britannica',
    url: 'https://www.britannica.com/did-you-know',
    handler: 'britannica',
  },
];

// ─── Category Mapping ──────────────────────────────────────────────
const CATEGORY_MAP = [
  { keywords: ['quantum', 'physics', 'particle', 'atom', 'energy', 'wave', 'gravity',
    'electromagnetic', 'nuclear', 'relativity', 'photon', 'electron'], category: 'Physics' },
  { keywords: ['dna', 'gene', 'cell', 'protein', 'organism', 'evolution', 'species',
    'bacteria', 'virus', 'enzyme', 'mutation', 'chromosome'], category: 'Biology' },
  { keywords: ['planet', 'star', 'galaxy', 'moon', 'asteroid', 'comet', 'nebula',
    'telescope', 'orbit', 'astronaut', 'mars', 'venus', 'jupiter'], category: 'Space' },
  { keywords: ['history', 'ancient', 'century', 'medieval', 'empire', 'war', 'king',
    'queen', 'dynasty', 'revolution', 'civilization'], category: 'History' },
  { keywords: ['brain', 'psychology', 'cognition', 'memory', 'emotion', 'behavior',
    'mental', 'neuron', 'perception', 'personality'], category: 'Psychology' },
  { keywords: ['philosophy', 'ethic', 'moral', 'logic', 'existential', 'consciousness',
    'reasoning', 'knowledge', 'truth'], category: 'Philosophy' },
  { keywords: ['startup', 'entrepreneur', 'venture', 'innovation', 'business', 'company',
    'founder', 'product', 'market'], category: 'Startups' },
  { keywords: ['ai', 'artificial intelligence', 'machine learning', 'neural', 'algorithm',
    'robot', 'deep learning', 'gpt', 'transformer'], category: 'AI' },
  { keywords: ['economy', 'market', 'trade', 'finance', 'inflation', 'gdp', 'capital',
    'tax', 'bank', 'currency', 'investment'], category: 'Economics' },
  { keywords: ['animal', 'plant', 'tree', 'forest', 'ocean', 'climate', 'ecosystem',
    'species', 'conservation', 'habitat', 'extinct'], category: 'Nature' },
  { keywords: ['computer', 'software', 'programming', 'internet', 'digital', 'data',
    'network', 'code', 'algorithm', 'chip', 'processor'], category: 'Technology' },
  { keywords: ['science', 'experiment', 'research', 'study', 'scientist', 'laboratory',
    'discovery', 'hypothesis', 'theory', 'scientific'], category: 'Science' },
];

function categorize(title, body) {
  const text = `${title} ${body}`.toLowerCase();
  for (const { keywords, category } of CATEGORY_MAP) {
    if (keywords.some(k => text.includes(k))) return category;
  }
  return 'Science'; // default
}

function estimateReadTime(text) {
  const words = text.split(/\s+/).length;
  return Math.max(8, Math.min(30, Math.round(words / 3)));
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

// ─── Source Handlers ──────────────────────────────────────────────
function extractWikipedia($) {
  const title = $('h1#firstHeading').text().trim();
  const bodyEl = $('p:not(.mw-empty-elt)').first();
  const body = bodyEl.text().trim();

  if (!title || !body || body.length < 60) return null;

  const clean = body.replace(/\[\d+\]/g, '').split('.')[0] + '.';
  return {
    title,
    body: clean.length > 20 ? clean : body.replace(/\[\d+\]/g, ''),
    source: 'Wikipedia',
    category: categorize(title, body),
    readTime: estimateReadTime(body),
    tags: `wikipedia,${categorize(title, body).toLowerCase()},curated`,
    likes: Math.floor(Math.random() * 500) + 100,
  };
}

function extractBritannica($) {
  const items = [];
  $('.did-you-know-item, .card-body').each((_, el) => {
    const title = $(el).find('h3, .title, strong').first().text().trim();
    const body = $(el).find('p, .description').first().text().trim();
    if (title && body && body.length > 30) {
      items.push({
        title,
        body: body.replace(/\[\d+\]/g, ''),
        source: 'Britannica',
        category: categorize(title, body),
        readTime: estimateReadTime(body),
        tags: `britannica,${categorize(title, body).toLowerCase()},curated`,
        likes: Math.floor(Math.random() * 300) + 50,
      });
    }
  });
  return items;
}

// ─── Main ──────────────────────────────────────────────────────────
async function main() {
  console.log('🔍 Curio Content Scraper (Crawlee)');
  console.log(`   Target: ${MAX_ITEMS} items`);
  if (DRY_RUN) console.log('   Mode: DRY RUN');
  console.log();

  const inserted = [];
  const skipped = [];

  // Create a Crawlee CheerioCrawler for each source
  for (const source of SOURCES) {
    if (inserted.length >= MAX_ITEMS) break;

    console.log(`📖 Crawling ${source.name}...`);

    const crawler = new CheerioCrawler({
      maxRequestsPerCrawl: 3,
      maxRequestRetries: 2,
      requestHandlerTimeoutSecs: 20,

      async requestHandler({ $, request, log }) {
        const items = [];

        if (source.handler === 'wikipedia') {
          const item = extractWikipedia($);
          if (item) items.push(item);
        } else if (source.handler === 'britannica') {
          items.push(...extractBritannica($));
        }

        for (const item of items) {
          if (inserted.length >= MAX_ITEMS) break;

          if (DRY_RUN) {
            console.log(`  📋 [${item.category}] ${item.title.slice(0, 70)}...`);
            inserted.push(item);
            continue;
          }

          const ok = await insertContent(item);
          if (ok) {
            inserted.push(item);
            console.log(`  ✓ [${item.category}] ${item.title.slice(0, 70)}...`);
          } else {
            skipped.push(item.title);
          }
        }
      },

      async failedRequestHandler({ request, log }) {
        log.warning(`Failed: ${request.url}`);
      },
    });

    await crawler.run([source.url]);
  }

  // Summary
  if (!DRY_RUN && pool) {
    const { rows } = await pool.query('SELECT COUNT(*) FROM contents');
    console.log(`\n✅ Done! Inserted ${inserted.length}, skipped ${skipped.length}. Total in DB: ${rows[0].count}`);
    await pool.end();
  } else if (DRY_RUN) {
    console.log(`\n📋 DRY RUN: Would insert ${inserted.length} items`);
  }

  if (inserted.length === 0 && skipped.length > 0) {
    console.log('ℹ All candidates were duplicates (already in DB). Try again later for new content.');
  }
}

main().catch(err => {
  console.error('❌ Fatal error:', err);
  process.exit(1);
});
