#!/usr/bin/env python3
"""
Curio Database Seed Script

Seeds the Curio PostgreSQL database with categories and knowledge content.
Can be run standalone or scheduled as a cron job for periodic data refresh.

Usage:
    python seed.py                          # Uses DATABASE_URL from .env or env var
    DATABASE_URL=postgres://... python seed.py
    python seed.py --reset                  # Drop and recreate tables before seeding
"""

import os
import sys
import argparse
from dotenv import load_dotenv

import psycopg2
from psycopg2.extras import execute_values

# Load .env from project root or backend directory
env_paths = [
    os.path.join(os.path.dirname(__file__), "..", "backend", ".env"),
    os.path.join(os.path.dirname(__file__), "..", ".env"),
]
for path in env_paths:
    if os.path.exists(path):
        load_dotenv(path)
        break

CATEGORIES = [
    {"name": "Science", "icon": "biotech", "color_hex": "#00f4fe", "priority": 1},
    {"name": "Space", "icon": "rocket_launch", "color_hex": "#a8cec8", "priority": 2},
    {"name": "History", "icon": "history_edu", "color_hex": "#e9c400", "priority": 3},
    {"name": "Biology", "icon": "psychology", "color_hex": "#63f7ff", "priority": 4},
    {"name": "Psychology", "icon": "psychology", "color_hex": "#c3eae4", "priority": 5},
    {"name": "Philosophy", "icon": "psychology", "color_hex": "#ffe16d", "priority": 6},
    {"name": "Physics", "icon": "atom", "color_hex": "#00dce5", "priority": 7},
    {"name": "Startups", "icon": "lightbulb", "color_hex": "#e9c400", "priority": 8},
    {"name": "AI", "icon": "neurology", "color_hex": "#00f4fe", "priority": 9},
    {"name": "Economics", "icon": "account_balance", "color_hex": "#63f7ff", "priority": 10},
    {"name": "Nature", "icon": "forest", "color_hex": "#a8cec8", "priority": 11},
    {"name": "Technology", "icon": "memory", "color_hex": "#00dce5", "priority": 12},
    {"name": "Poetry", "icon": "auto_stories", "color_hex": "#f472b6", "priority": 13},
    {"name": "Movies", "icon": "movie", "color_hex": "#fb923c", "priority": 14},
    {"name": "Neuroscience", "icon": "psychology", "color_hex": "#a78bfa", "priority": 15},
    {"name": "Literature", "icon": "menu_book", "color_hex": "#fbbf24", "priority": 16},
    {"name": "Geography", "icon": "public", "color_hex": "#34d399", "priority": 17},
    {"name": "Music", "icon": "music_note", "color_hex": "#f472b6", "priority": 18},
    {"name": "Sports", "icon": "sports_soccer", "color_hex": "#fb923c", "priority": 19},
    {"name": "Food", "icon": "ramen_dining", "color_hex": "#f59e0b", "priority": 20},
]

CONTENT = [
    # Science (category_id: 1)
    {"category_id": 1, "title": "A day on Venus is longer than a year on Venus", "body": "Venus rotates so slowly on its axis that it takes 243 Earth days to complete one rotation, while orbiting the Sun in just 225 Earth days. This means a single day on Venus is actually longer than its entire year.", "source": "NASA Solar System Exploration", "read_time_secs": 15, "tags": "venus,planets,solar-system", "likes": 4200},
    {"category_id": 1, "title": "Octopuses have three hearts", "body": "Two pump blood to the gills, while the third pumps it to the rest of the body. When an octopus swims, the heart that delivers blood to the body stops beating, which explains why they prefer crawling over swimming.", "source": "National Geographic", "read_time_secs": 10, "tags": "octopus,marine-biology,animals", "likes": 3800},
    {"category_id": 1, "title": "Bananas are technically berries, but strawberries aren't", "body": "Botanically, a berry is a fruit produced from the ovary of a single flower with seeds embedded in the flesh. Bananas fit this definition perfectly. Strawberries, however, are 'aggregate fruits' because they form from multiple ovaries of a single flower.", "source": "Botanical Journal", "read_time_secs": 12, "tags": "botany,food,science", "likes": 3100},

    # Space (category_id: 2)
    {"category_id": 2, "title": "A spoonful of neutron star weighs about 6 billion tons", "body": "Neutron stars are the collapsed cores of massive stars. They pack a mass greater than the Sun into a sphere roughly the size of a city. Just one teaspoon of neutron star material would weigh approximately 6 billion tons on Earth.", "source": "NASA Astrophysics", "read_time_secs": 15, "tags": "neutron-stars,astrophysics,space", "likes": 5600},
    {"category_id": 2, "title": "There's a giant cloud of alcohol in space", "body": "The Sagittarius B2 cloud, located near the center of the Milky Way, contains billions of liters of ethyl alcohol. It's enough to fill 400 trillion pints of beer. Unfortunately, it also contains toxic chemicals that make it undrinkable.", "source": "ESA Astronomy", "read_time_secs": 12, "tags": "space-clouds,milky-way,alcohol", "likes": 4800},
    {"category_id": 2, "title": "Footprints on the Moon will last 100 million years", "body": "The Moon has no atmosphere, meaning no wind or water erosion. Unless a meteorite directly hits the Apollo landing sites, the footprints left by astronauts will remain visible for at least 100 million years.", "source": "NASA Lunar Science", "read_time_secs": 10, "tags": "moon,apollo,space-exploration", "likes": 6200},

    # History (category_id: 3)
    {"category_id": 3, "title": "The Great Pyramid was built over 3,800 years ago", "body": "The Great Pyramid of Giza was completed around 2560 BC and remained the tallest man-made structure in the world for over 3,800 years. It was built using approximately 2.3 million blocks of stone, each weighing between 2.5 and 15 tons.", "source": "World History Encyclopedia", "read_time_secs": 15, "tags": "pyramids,egypt,ancient-civilizations", "likes": 3400},
    {"category_id": 3, "title": "The first email was sent in 1971", "body": "Ray Tomlinson sent the first email on ARPANET in 1971. He chose the '@' symbol to separate the user from the machine, creating the email addressing format we still use today. The content of that first email was something like 'QWERTYUIOP' or a similar test message.", "source": "Internet History", "read_time_secs": 10, "tags": "email,internet,technology-history", "likes": 2800},
    {"category_id": 3, "title": "Cleopatra lived closer to the Moon landing than to the Pyramid construction", "body": "Cleopatra VII lived around 30 BC. The Great Pyramid was built around 2560 BC. The Moon landing was in 1969 AD. So Cleopatra is separated from the Pyramids by ~2,530 years, but from the Moon landing by only ~1,999 years.", "source": "Historical Timeline", "read_time_secs": 10, "tags": "cleopatra,pyramids,moon,perspective", "likes": 5100},

    # Biology (category_id: 4)
    {"category_id": 4, "title": "Your body replaces every cell every 7 years", "body": "Different cells regenerate at different rates. Skin cells replace every 2-3 weeks, liver cells every 150-500 days, but some heart and brain cells last a lifetime. The claim that you get a completely new body every 7 years is a simplification.", "source": "Cell Biology Journal", "read_time_secs": 12, "tags": "cells,regeneration,biology", "likes": 2700},
    {"category_id": 4, "title": "Trees communicate through an underground network called the Wood Wide Web", "body": "Through mycorrhizal networks—symbiotic connections between tree roots and fungi—trees can share nutrients, send warning signals about pests, and even transfer carbon to neighboring trees in need. The largest known organism is a honey fungus in Oregon spanning 2.4 miles.", "source": "The Hidden Life of Trees", "read_time_secs": 15, "tags": "trees,fungi,communication,nature", "likes": 3800},

    # Psychology (category_id: 5)
    {"category_id": 5, "title": "The Dunning-Kruger Effect makes unskilled people overestimate their abilities", "body": "People with low ability at a task tend to overestimate their skill, while experts tend to underestimate theirs. This cognitive bias was identified by psychologists David Dunning and Justin Kruger in 1999 and explains why beginners often feel overconfident.", "source": "Journal of Personality and Social Psychology", "read_time_secs": 15, "tags": "cognitive-bias,psychology,self-awareness", "likes": 3600},
    {"category_id": 5, "title": "Your brain rewires itself every time you learn something new", "body": "Neuroplasticity is the brain's ability to reorganize itself by forming new neural connections throughout life. Learning a new skill, language, or even recovering from a brain injury literally changes the physical structure of your brain.", "source": "Neuroscience Research", "read_time_secs": 12, "tags": "neuroplasticity,brain,learning", "likes": 2900},

    # Philosophy (category_id: 6)
    {"category_id": 6, "title": "The Ship of Theseus paradox asks: when does something stop being itself?", "body": "If you replace every plank of a ship over time, is it still the same ship? This ancient paradox explores identity and change. It's relevant today to debates about identity, consciousness, and even software updates.", "source": "Plutarch, Parallel Lives", "read_time_secs": 15, "tags": "identity,paradox,philosophy", "likes": 3200},
    {"category_id": 6, "title": "Stoicism teaches that we can't control events, only our responses", "body": "The Stoic philosophy, founded in Athens by Zeno of Citium around 300 BC, teaches that while we cannot control external events, we have complete control over our judgments and responses. This core idea has influenced cognitive behavioral therapy.", "source": "Meditations by Marcus Aurelius", "read_time_secs": 12, "tags": "stoicism,control,resilience", "likes": 4100},

    # Physics (category_id: 7)
    {"category_id": 7, "title": "Quantum entanglement lets particles communicate instantly across any distance", "body": "When two particles become entangled, measuring one instantly determines the state of the other, regardless of distance. Einstein called it 'spooky action at a distance.' This phenomenon is the foundation for quantum computing and quantum cryptography.", "source": "Physics Today", "read_time_secs": 15, "tags": "quantum,entanglement,physics", "likes": 4400},
    {"category_id": 7, "title": "Light takes 8 minutes and 20 seconds to reach Earth from the Sun", "body": "When you look at the Sun, you're seeing it as it was 8.3 minutes ago. If the Sun were to suddenly disappear, we wouldn't know about it for over 8 minutes. The nearest star, Proxima Centauri, is 4.24 light-years away.", "source": "NASA Heliophysics", "read_time_secs": 10, "tags": "light,speed-of-light,sun", "likes": 3500},

    # Startups (category_id: 8)
    {"category_id": 8, "title": "Airbnb founders sold cereal boxes to keep their startup alive", "body": "During the 2008 Democratic National Convention, Airbnb founders Brian Chesky and Joe Gebbia created limited-edition 'Obama O's' and 'Cap'n McCain's' cereal boxes. They sold them for $40 each and raised over $30,000, keeping the company alive during its darkest days.", "source": "Airbnb Founding Story", "read_time_secs": 15, "tags": "airbnb,startup,cereal,perseverance", "likes": 6700},
    {"category_id": 8, "title": "Slack was built from a failed video game company", "body": "Slack started as an internal communication tool for Tiny Speck, a company building a game called Glitch. When Glitch failed, the team realized their chat tool was more valuable than the game. They pivoted and sold Slack for $27.7 billion to Salesforce.", "source": "Slack History", "read_time_secs": 12, "tags": "slack,pivot,failure,success", "likes": 5300},
    {"category_id": 8, "title": "The first computer bug was an actual moth", "body": "In 1947, engineers at Harvard found a moth trapped in the Mark II computer's relay. They taped it into the logbook with the note 'First actual case of bug being found.' This is where the term 'debugging' comes from.", "source": "Naval History Museum", "read_time_secs": 10, "tags": "computer-history,bug,debugging", "likes": 4500},

    # AI (category_id: 9)
    {"category_id": 9, "title": "Transformer models changed AI by processing words simultaneously", "body": "Introduced in 2017, the Transformer architecture processes all words in a sequence simultaneously rather than sequentially. This parallel processing enables models like GPT and BERT to understand context far better than previous approaches.", "source": "Attention Is All You Need (Vaswani et al.)", "read_time_secs": 15, "tags": "transformers,deep-learning,ai", "likes": 3900},
    {"category_id": 9, "title": "GPT-3 has 175 billion parameters", "body": "GPT-3, released by OpenAI in 2020, has 175 billion parameters—roughly 100x more than its predecessor GPT-2. Training it required thousands of GPUs running for weeks and cost an estimated $4.6 million in compute alone.", "source": "OpenAI Papers", "read_time_secs": 12, "tags": "gpt-3,large-language-models,compute", "likes": 3400},

    # Economics (category_id: 10)
    {"category_id": 10, "title": "The Tulip Mania of 1637 saw a single tulip bulb cost more than a house", "body": "During the Dutch Golden Age, tulip bulbs became a speculative asset. At the peak of the mania, a single bulb of the 'Semper Augustus' tulip sold for 6,000 guilders—more than the cost of a canal house in Amsterdam. It's considered the first recorded speculative bubble.", "source": "Economic History Review", "read_time_secs": 15, "tags": "tulip-mania,bubbles,economics", "likes": 2600},
    {"category_id": 10, "title": "Bitcoin uses more electricity than entire countries", "body": "Bitcoin mining consumes approximately 150 terawatt-hours of electricity annually—more than the entire country of Argentina. A single Bitcoin transaction uses enough energy to power an average U.S. household for over a month.", "source": "Cambridge Bitcoin Electricity Consumption Index", "read_time_secs": 12, "tags": "bitcoin,cryptocurrency,energy", "likes": 3100},

    # Nature (category_id: 11)
    {"category_id": 11, "title": "Honey never spoils—edible honey has been found in ancient Egyptian tombs", "body": "Archaeologists discovered 3,000-year-old jars of honey in Egyptian tombs that were still perfectly edible. Honey's low water content, acidic pH, and natural hydrogen peroxide production create an environment where bacteria cannot survive.", "source": "Smithsonian Magazine", "read_time_secs": 12, "tags": "honey,preservation,egypt", "likes": 4900},
    {"category_id": 11, "title": "The Amazon rainforest produces 20% of the world's oxygen", "body": "The Amazon spans 5.5 million square kilometers across 9 countries. It's home to 10% of the world's known species and produces approximately 20% of the Earth's oxygen. It also stores 150-200 billion tons of carbon.", "source": "World Wildlife Fund", "read_time_secs": 10, "tags": "amazon,rainforest,oxygen,climate", "likes": 3300},

    # Technology (category_id: 12)
    {"category_id": 12, "title": "More computing power exists in a smartphone than in the Apollo 11 spacecraft", "body": "The Apollo Guidance Computer had 64KB of memory and operated at 0.043 MHz. Your smartphone has billions of transistors, multi-core processors running at GHz speeds, and more computing power than NASA had when they sent humans to the Moon.", "source": "NASA Computing History", "read_time_secs": 10, "tags": "smartphone,apollo,computing-power", "likes": 5800},
    {"category_id": 12, "title": "The World Wide Web was invented in 1989 by Tim Berners-Lee", "body": "Working at CERN, Tim Berners-Lee proposed a system of interlinked hypertext documents accessible via the internet. The first website—info.cern.ch—went live in 1991. He gave away the technology for free, refusing to patent it.", "source": "CERN Web History", "read_time_secs": 12, "tags": "world-wide-web,internet,invention", "likes": 3100},

    # Quotes (category_id: 6 - Philosophy)
    {"category_id": 6, "title": '"The only true wisdom is in knowing you know nothing."', "body": "Socrates challenged people to examine their beliefs and admit their ignorance. This quote represents the foundation of critical thinking and intellectual humility—recognizing the limits of our knowledge is the beginning of wisdom.", "source": "Plato, Apology", "read_time_secs": 10, "tags": "socrates,wisdom,knowledge", "likes": 7100},
    {"category_id": 8, "title": '"The best way to predict the future is to invent it."', "body": "Alan Kay, a pioneering computer scientist, believed in proactive creation rather than passive forecasting. This philosophy drove innovation at Xerox PARC, where the graphical user interface, object-oriented programming, and the modern PC were born.", "source": "Alan Kay, PARC", "read_time_secs": 10, "tags": "alan-kay,innovation,future", "likes": 5900},
    {"category_id": 1, "title": '"Imagination is more important than knowledge."', "body": "Einstein believed that while knowledge defines what we currently know, imagination points toward what we can discover. Knowledge is limited—imagination encircles the world and drives every scientific breakthrough.", "source": "Albert Einstein", "read_time_secs": 10, "tags": "einstein,imagination,knowledge", "likes": 6400},
    {"category_id": 5, "title": '"The mind is everything. What you think you become."', "body": "This quote captures the essence of cognitive psychology: our thoughts shape our reality. Modern CBT (Cognitive Behavioral Therapy) is built on this principle—changing thought patterns can change behaviors, emotions, and outcomes.", "source": "Buddha", "read_time_secs": 10, "tags": "mind,thoughts,psychology", "likes": 5200},
]


CREATE_CATEGORIES_TABLE = """
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    icon VARCHAR(100) DEFAULT '',
    color_hex VARCHAR(7) DEFAULT '',
    priority INTEGER DEFAULT 0
);
"""

CREATE_CONTENT_TABLE = """
CREATE TABLE IF NOT EXISTS contents (
    id SERIAL PRIMARY KEY,
    category_id INTEGER REFERENCES categories(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    source TEXT DEFAULT '',
    source_url TEXT DEFAULT '',
    read_time_secs INTEGER DEFAULT 15,
    tags TEXT DEFAULT '',
    likes INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_contents_title ON contents(title);
"""

INSERT_CATEGORY = """
INSERT INTO categories (name, icon, color_hex, priority)
VALUES (%s, %s, %s, %s)
ON CONFLICT (name) DO NOTHING;
"""

INSERT_CONTENT = """
INSERT INTO contents (category_id, title, body, source, read_time_secs, tags, likes)
VALUES (%s, %s, %s, %s, %s, %s, %s)
ON CONFLICT (title) DO NOTHING;
"""



def get_db_url() -> str:
    url = os.getenv("DATABASE_URL")
    if not url:
        print("ERROR: DATABASE_URL not set. Create a backend/.env file or set the env var.")
        print("Example: postgres://postgres:password@localhost:5432/curio?sslmode=disable")
        sys.exit(1)
    return url


def connect(db_url: str):
    conn = psycopg2.connect(db_url)
    conn.autocommit = True
    return conn


def migrate(conn):
    with conn.cursor() as cur:
        cur.execute(CREATE_CATEGORIES_TABLE)
        cur.execute(CREATE_CONTENT_TABLE)
    print("✓ Tables ensured")


def seed_categories(conn):
    count = 0
    with conn.cursor() as cur:
        for cat in CATEGORIES:
            cur.execute(INSERT_CATEGORY, (cat["name"], cat["icon"], cat["color_hex"], cat["priority"]))
            if cur.rowcount > 0:
                count += 1
                print(f"  Created category: {cat['name']}")
    print(f"✓ Categories: {count} new, {len(CATEGORIES) - count} existing")


def seed_content(conn):
    count = 0
    with conn.cursor() as cur:
        for item in CONTENT:
            cur.execute(INSERT_CONTENT, (
                item["category_id"],
                item["title"],
                item["body"],
                item["source"],
                item["read_time_secs"],
                item["tags"],
                item["likes"],
            ))
            if cur.rowcount > 0:
                count += 1
    print(f"✓ Content: {count} new, {len(CONTENT) - count} existing")


def reset_tables(conn):
    with conn.cursor() as cur:
        cur.execute("DROP TABLE IF EXISTS contents CASCADE;")
        cur.execute("DROP TABLE IF EXISTS categories CASCADE;")
    print("✓ Tables dropped")
    migrate(conn)


def verify(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM categories;")
        cat_count = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM contents;")
        con_count = cur.fetchone()[0]
    print(f"\n✓ Verification: {cat_count} categories, {con_count} content items in database")


def main():
    parser = argparse.ArgumentParser(description="Seed Curio database with content")
    parser.add_argument("--reset", action="store_true", help="Drop and recreate tables before seeding")
    args = parser.parse_args()

    db_url = get_db_url()
    conn = connect(db_url)
    print(f"Connected to PostgreSQL\n")

    if args.reset:
        reset_tables(conn)
    else:
        migrate(conn)

    print("Seeding categories...")
    seed_categories(conn)

    print("Seeding content...")
    seed_content(conn)

    verify(conn)
    conn.close()
    print("\nSeed complete!")


if __name__ == "__main__":
    main()
