#!/usr/bin/env python3
"""
Curio Database Seed Script

Seeds the Curio PostgreSQL database with categories and per-category content tables.
After migration (scripts/migrate_per_category.sql), the 'contents' table is a VIEW.

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
    {"name": "Science", "icon": "biotech", "color_hex": "#00f4fe", "priority": 1, "table_id": 1},
    {"name": "Space", "icon": "rocket_launch", "color_hex": "#a8cec8", "priority": 2, "table_id": 2},
    {"name": "History", "icon": "history_edu", "color_hex": "#e9c400", "priority": 3, "table_id": 3},
    {"name": "Biology", "icon": "eco", "color_hex": "#63f7ff", "priority": 4, "table_id": 4},
    {"name": "Psychology", "icon": "psychology", "color_hex": "#c3eae4", "priority": 5, "table_id": 5},
    {"name": "Philosophy", "icon": "balance_scale", "color_hex": "#ffe16d", "priority": 6, "table_id": 6},
    {"name": "Physics", "icon": "atom", "color_hex": "#00dce5", "priority": 7, "table_id": 7},
    {"name": "Startups", "icon": "lightbulb", "color_hex": "#e9c400", "priority": 8, "table_id": 8},
    {"name": "AI", "icon": "smart_toy", "color_hex": "#00f4fe", "priority": 9, "table_id": 9},
    {"name": "Economics", "icon": "account_balance", "color_hex": "#63f7ff", "priority": 10, "table_id": 10},
    {"name": "Nature", "icon": "forest", "color_hex": "#a8cec8", "priority": 11, "table_id": 11},
    {"name": "Technology", "icon": "computer", "color_hex": "#00dce5", "priority": 12, "table_id": 12},
    {"name": "Poetry", "icon": "auto_stories", "color_hex": "#f472b6", "priority": 13, "table_id": 25, "l1_category": "Poems"},
    {"name": "Movies", "icon": "movie", "color_hex": "#fb923c", "priority": 14, "table_id": 26},
    {"name": "Neuroscience", "icon": "microscope", "color_hex": "#a78bfa", "priority": 15, "table_id": 27},
    {"name": "Literature", "icon": "menu_book", "color_hex": "#fbbf24", "priority": 16, "table_id": 28},
    {"name": "Geography", "icon": "public", "color_hex": "#34d399", "priority": 17, "table_id": 29},
    {"name": "Music", "icon": "music_note", "color_hex": "#f472b6", "priority": 18, "table_id": 30},
    {"name": "Sports", "icon": "sports_soccer", "color_hex": "#fb923c", "priority": 19, "table_id": 31},
    {"name": "Food", "icon": "ramen_dining", "color_hex": "#f59e0b", "priority": 20, "table_id": 32},
    {"name": "Shayari", "icon": "edit_note", "color_hex": "#d946ef", "priority": 21, "table_id": 33, "l1_category": "Poems"},
    {"name": "Puzzles", "icon": "extension", "color_hex": "#f97316", "priority": 22, "table_id": 34, "l1_category": "Puzzles"},
    {"name": "Short Stories", "icon": "article", "color_hex": "#06b6d4", "priority": 23, "table_id": 35, "l1_category": "Short Stories"},

    # ── Poems subcategories ──────────────────────────────────────
    {"name": "Hindi Poetry", "icon": "auto_stories", "color_hex": "#f472b6", "priority": 24, "table_id": 36, "l1_category": "Poems"},

    # ── Puzzles subcategories ────────────────────────────────────
    {"name": "Sudoku", "icon": "grid_on", "color_hex": "#f97316", "priority": 25, "table_id": 37, "l1_category": "Puzzles"},
    {"name": "Math Puzzles", "icon": "calculate", "color_hex": "#fb923c", "priority": 26, "table_id": 38, "l1_category": "Puzzles"},
    {"name": "Logic Puzzles", "icon": "psychology", "color_hex": "#a78bfa", "priority": 27, "table_id": 39, "l1_category": "Puzzles"},
    {"name": "Word Puzzles", "icon": "abc", "color_hex": "#fbbf24", "priority": 28, "table_id": 40, "l1_category": "Puzzles"},

    # ── Short Stories subcategories ──────────────────────────────
    {"name": "Classic Fiction", "icon": "menu_book", "color_hex": "#06b6d4", "priority": 29, "table_id": 41, "l1_category": "Short Stories"},
    {"name": "Micro Stories", "icon": "auto_stories", "color_hex": "#34d399", "priority": 30, "table_id": 42, "l1_category": "Short Stories"},
    {"name": "Serialized Stories", "icon": "library_books", "color_hex": "#6366f1", "priority": 31, "table_id": 43, "l1_category": "Short Stories"},
]

CONTENT = [
    {"category_id": 1, "title": "A day on Venus is longer than a year on Venus", "body": "Venus rotates so slowly on its axis that it takes 243 Earth days to complete one rotation, while orbiting the Sun in just 225 Earth days. This means a single day on Venus is actually longer than its entire year.", "source": "NASA Solar System Exploration", "read_time_secs": 15, "tags": "venus,planets,solar-system", "likes": 4200, "description": "", "poet": ""},
    {"category_id": 1, "title": "Octopuses have three hearts", "body": "Two pump blood to the gills, while the third pumps it to the rest of the body. When an octopus swims, the heart that delivers blood to the body stops beating, which explains why they prefer crawling over swimming.", "source": "National Geographic", "read_time_secs": 10, "tags": "octopus,marine-biology,animals", "likes": 3800, "description": "", "poet": ""},
    {"category_id": 1, "title": "Bananas are technically berries, but strawberries aren't", "body": "Botanically, a berry is a fruit produced from the ovary of a single flower with seeds embedded in the flesh. Bananas fit this definition perfectly. Strawberries, however, are 'aggregate fruits' because they form from multiple ovaries of a single flower.", "source": "Botanical Journal", "read_time_secs": 12, "tags": "botany,food,science", "likes": 3100, "description": "", "poet": ""},
    {"category_id": 2, "title": "A spoonful of neutron star weighs about 6 billion tons", "body": "Neutron stars are the collapsed cores of massive stars. They pack a mass greater than the Sun into a sphere roughly the size of a city. Just one teaspoon of neutron star material would weigh approximately 6 billion tons on Earth.", "source": "NASA Astrophysics", "read_time_secs": 15, "tags": "neutron-stars,astrophysics,space", "likes": 5600, "description": "", "poet": ""},
    {"category_id": 2, "title": "There's a giant cloud of alcohol in space", "body": "The Sagittarius B2 cloud, located near the center of the Milky Way, contains billions of liters of ethyl alcohol. It's enough to fill 400 trillion pints of beer. Unfortunately, it also contains toxic chemicals that make it undrinkable.", "source": "ESA Astronomy", "read_time_secs": 12, "tags": "space-clouds,milky-way,alcohol", "likes": 4800, "description": "", "poet": ""},
    {"category_id": 2, "title": "Footprints on the Moon will last 100 million years", "body": "The Moon has no atmosphere, meaning no wind or water erosion. Unless a meteorite directly hits the Apollo landing sites, the footprints left by astronauts will remain visible for at least 100 million years.", "source": "NASA Lunar Science", "read_time_secs": 10, "tags": "moon,apollo,space-exploration", "likes": 6200, "description": "", "poet": ""},
    {"category_id": 3, "title": "The Great Pyramid was built over 3,800 years ago", "body": "The Great Pyramid of Giza was completed around 2560 BC and remained the tallest man-made structure in the world for over 3,800 years. It was built using approximately 2.3 million blocks of stone, each weighing between 2.5 and 15 tons.", "source": "World History Encyclopedia", "read_time_secs": 15, "tags": "pyramids,egypt,ancient-civilizations", "likes": 3400, "description": "", "poet": ""},
    {"category_id": 3, "title": "The first email was sent in 1971", "body": "Ray Tomlinson sent the first email on ARPANET in 1971. He chose the '@' symbol to separate the user from the machine, creating the email addressing format we still use today. The content of that first email was something like 'QWERTYUIOP' or a similar test message.", "source": "Internet History", "read_time_secs": 10, "tags": "email,internet,technology-history", "likes": 2800, "description": "", "poet": ""},
    {"category_id": 3, "title": "Cleopatra lived closer to the Moon landing than to the Pyramid construction", "body": "Cleopatra VII lived around 30 BC. The Great Pyramid was built around 2560 BC. The Moon landing was in 1969 AD. So Cleopatra is separated from the Pyramids by ~2,530 years, but from the Moon landing by only ~1,999 years.", "source": "Historical Timeline", "read_time_secs": 10, "tags": "cleopatra,pyramids,moon,perspective", "likes": 5100, "description": "", "poet": ""},
    {"category_id": 4, "title": "Your body replaces every cell every 7 years", "body": "Different cells regenerate at different rates. Skin cells replace every 2-3 weeks, liver cells every 150-500 days, but some heart and brain cells last a lifetime. The claim that you get a completely new body every 7 years is a simplification.", "source": "Cell Biology Journal", "read_time_secs": 12, "tags": "cells,regeneration,biology", "likes": 2700, "description": "", "poet": ""},
    {"category_id": 4, "title": "Trees communicate through an underground network called the Wood Wide Web", "body": "Through mycorrhizal networks\u2014symbiotic connections between tree roots and fungi\u2014trees can share nutrients, send warning signals about pests, and even transfer carbon to neighboring trees in need. The largest known organism is a honey fungus in Oregon spanning 2.4 miles.", "source": "The Hidden Life of Trees", "read_time_secs": 15, "tags": "trees,fungi,communication,nature", "likes": 3800, "description": "", "poet": ""},
    {"category_id": 5, "title": "The Dunning-Kruger Effect makes unskilled people overestimate their abilities", "body": "People with low ability at a task tend to overestimate their skill, while experts tend to underestimate theirs. This cognitive bias was identified by psychologists David Dunning and Justin Kruger in 1999 and explains why beginners often feel overconfident.", "source": "Journal of Personality and Social Psychology", "read_time_secs": 15, "tags": "cognitive-bias,psychology,self-awareness", "likes": 3600, "description": "", "poet": ""},
    {"category_id": 5, "title": "Your brain rewires itself every time you learn something new", "body": "Neuroplasticity is the brain's ability to reorganize itself by forming new neural connections throughout life. Learning a new skill, language, or even recovering from a brain injury literally changes the physical structure of your brain.", "source": "Neuroscience Research", "read_time_secs": 12, "tags": "neuroplasticity,brain,learning", "likes": 2900, "description": "", "poet": ""},
    {"category_id": 6, "title": "\u201cThe only true wisdom is in knowing you know nothing.\u201d", "body": "Socrates challenged people to examine their beliefs and admit their ignorance.", "source": "Plato, Apology", "read_time_secs": 10, "tags": "socrates,wisdom,knowledge", "likes": 7100, "description": "", "poet": ""},
    {"category_id": 6, "title": "The Ship of Theseus paradox asks: when does something stop being itself?", "body": "If you replace every plank of a ship over time, is it still the same ship? This ancient paradox explores identity and change. It's relevant today to debates about identity, consciousness, and even software updates.", "source": "Plutarch, Parallel Lives", "read_time_secs": 15, "tags": "identity,paradox,philosophy", "likes": 3200, "description": "", "poet": ""},
    {"category_id": 6, "title": "Stoicism teaches that we can't control events, only our responses", "body": "The Stoic philosophy, founded in Athens by Zeno of Citium around 300 BC, teaches that while we cannot control external events, we have complete control over our judgments and responses. This core idea has influenced cognitive behavioral therapy.", "source": "Meditations by Marcus Aurelius", "read_time_secs": 12, "tags": "stoicism,control,resilience", "likes": 4100, "description": "", "poet": ""},
    {"category_id": 7, "title": "Quantum entanglement lets particles communicate instantly across any distance", "body": "When two particles become entangled, measuring one instantly determines the state of the other, regardless of distance. Einstein called it 'spooky action at a distance.' This phenomenon is the foundation for quantum computing and quantum cryptography.", "source": "Physics Today", "read_time_secs": 15, "tags": "quantum,entanglement,physics", "likes": 4400, "description": "", "poet": ""},
    {"category_id": 7, "title": "Light takes 8 minutes and 20 seconds to reach Earth from the Sun", "body": "When you look at the Sun, you're seeing it as it was 8.3 minutes ago. If the Sun were to suddenly disappear, we wouldn't know about it for over 8 minutes. The nearest star, Proxima Centauri, is 4.24 light-years away.", "source": "NASA Heliophysics", "read_time_secs": 10, "tags": "light,speed-of-light,sun", "likes": 3500, "description": "", "poet": ""},
    {"category_id": 8, "title": "Airbnb founders sold cereal boxes to keep their startup alive", "body": "During the 2008 Democratic National Convention, Airbnb founders Brian Chesky and Joe Gebbia created limited-edition 'Obama O's' and 'Cap'n McCain's' cereal boxes. They sold them for $40 each and raised over $30,000, keeping the company alive during its darkest days.", "source": "Airbnb Founding Story", "read_time_secs": 15, "tags": "airbnb,startup,cereal,perseverance", "likes": 6700, "description": "", "poet": ""},
    {"category_id": 8, "title": "Slack was built from a failed video game company", "body": "Slack started as an internal communication tool for Tiny Speck, a company building a game called Glitch. When Glitch failed, the team realized their chat tool was more valuable than the game. They pivoted and sold Slack for $27.7 billion to Salesforce.", "source": "Slack History", "read_time_secs": 12, "tags": "slack,pivot,failure,success", "likes": 5300, "description": "", "poet": ""},
    {"category_id": 8, "title": "The first computer bug was an actual moth", "body": "In 1947, engineers at Harvard found a moth trapped in the Mark II computer's relay. They taped it into the logbook with the note 'First actual case of bug being found.' This is where the term 'debugging' comes from.", "source": "Naval History Museum", "read_time_secs": 10, "tags": "computer-history,bug,debugging", "likes": 4500, "description": "", "poet": ""},
    {"category_id": 8, "title": "\u201cThe best way to predict the future is to invent it.\u201d", "body": "Alan Kay believed in proactive creation rather than passive forecasting.", "source": "Alan Kay, PARC", "read_time_secs": 10, "tags": "alan-kay,innovation,future", "likes": 5900, "description": "", "poet": ""},
    {"category_id": 1, "title": "\u201cImagination is more important than knowledge.\u201d", "body": "Einstein believed that while knowledge defines what we currently know, imagination points toward what we can discover.", "source": "Albert Einstein", "read_time_secs": 10, "tags": "einstein,imagination,knowledge", "likes": 6400, "description": "", "poet": ""},
    {"category_id": 5, "title": "\u201cThe mind is everything. What you think you become.\u201d", "body": "This captures cognitive psychology: our thoughts shape our reality.", "source": "Buddha", "read_time_secs": 10, "tags": "mind,thoughts,psychology", "likes": 5200, "description": "", "poet": ""},
    {"category_id": 9, "title": "Transformer models changed AI by processing words simultaneously", "body": "Introduced in 2017, the Transformer architecture processes all words in a sequence simultaneously rather than sequentially. This parallel processing enables models like GPT and BERT to understand context far better than previous approaches.", "source": "Attention Is All You Need (Vaswani et al.)", "read_time_secs": 15, "tags": "transformers,deep-learning,ai", "likes": 3900, "description": "", "poet": ""},
    {"category_id": 9, "title": "GPT-3 has 175 billion parameters", "body": "GPT-3, released by OpenAI in 2020, has 175 billion parameters\u2014roughly 100x more than its predecessor GPT-2. Training it required thousands of GPUs running for weeks and cost an estimated $4.6 million in compute alone.", "source": "OpenAI Papers", "read_time_secs": 12, "tags": "gpt-3,large-language-models,compute", "likes": 3400, "description": "", "poet": ""},
    {"category_id": 10, "title": "The Tulip Mania of 1637 saw a single tulip bulb cost more than a house", "body": "During the Dutch Golden Age, tulip bulbs became a speculative asset. At the peak of the mania, a single bulb of the 'Semper Augustus' tulip sold for 6,000 guilders\u2014more than the cost of a canal house in Amsterdam. It's considered the first recorded speculative bubble.", "source": "Economic History Review", "read_time_secs": 15, "tags": "tulip-mania,bubbles,economics", "likes": 2600, "description": "", "poet": ""},
    {"category_id": 10, "title": "Bitcoin uses more electricity than entire countries", "body": "Bitcoin mining consumes approximately 150 terawatt-hours of electricity annually\u2014more than the entire country of Argentina. A single Bitcoin transaction uses enough energy to power an average U.S. household for over a month.", "source": "Cambridge Bitcoin Electricity Consumption Index", "read_time_secs": 12, "tags": "bitcoin,cryptocurrency,energy", "likes": 3100, "description": "", "poet": ""},
    {"category_id": 11, "title": "Honey never spoils\u2014edible honey has been found in ancient Egyptian tombs", "body": "Archaeologists discovered 3,000-year-old jars of honey in Egyptian tombs that were still perfectly edible. Honey's low water content, acidic pH, and natural hydrogen peroxide production create an environment where bacteria cannot survive.", "source": "Smithsonian Magazine", "read_time_secs": 12, "tags": "honey,preservation,egypt", "likes": 4900, "description": "", "poet": ""},
    {"category_id": 11, "title": "The Amazon rainforest produces 20% of the world's oxygen", "body": "The Amazon spans 5.5 million square kilometers across 9 countries. It's home to 10% of the world's known species and produces approximately 20% of the Earth's oxygen. It also stores 150-200 billion tons of carbon.", "source": "World Wildlife Fund", "read_time_secs": 10, "tags": "amazon,rainforest,oxygen,climate", "likes": 3300, "description": "", "poet": ""},
    {"category_id": 12, "title": "More computing power exists in a smartphone than in the Apollo 11 spacecraft", "body": "The Apollo Guidance Computer had 64KB of memory and operated at 0.043 MHz. Your smartphone has billions of transistors, multi-core processors running at GHz speeds, and more computing power than NASA had when they sent humans to the Moon.", "source": "NASA Computing History", "read_time_secs": 10, "tags": "smartphone,apollo,computing-power", "likes": 5800, "description": "", "poet": ""},
    {"category_id": 12, "title": "The World Wide Web was invented in 1989 by Tim Berners-Lee", "body": "Working at CERN, Tim Berners-Lee proposed a system of interlinked hypertext documents accessible via the internet. The first website\u2014info.cern.ch\u2014went live in 1991. He gave away the technology for free, refusing to patent it.", "source": "CERN Web History", "read_time_secs": 12, "tags": "world-wide-web,internet,invention", "likes": 3100, "description": "", "poet": ""},
    # Poetry (category_id: 25) - English poetry from renowned poets
    {"category_id": 25, "title": "The Road Not Taken", "body": "Two roads diverged in a yellow wood, / And sorry I could not travel both / And be one traveler, long I stood / And looked down one as far as I could / To where it bent in the undergrowth; / ... / I took the one less traveled by, / And that has made all the difference.", "source": "Robert Frost, Mountain Interval (1916)", "read_time_secs": 15, "tags": "poetry,frost,choices,life", "likes": 9800, "description": "One of Frost's most beloved poems explores the human experience of choice and its lasting impact on our lives. Often misinterpreted as a simple celebration of individualism, the poem's speaker acknowledges with a sigh that both paths were equally worn, and the choice itself\u2014not the path\u2014is what makes all the difference.", "poet": "Robert Frost"},
    {"category_id": 25, "title": "I Wandered Lonely as a Cloud", "body": "I wandered lonely as a cloud / That floats on high o'er vales and hills, / When all at once I saw a crowd, / A host, of golden daffodils; / Beside the lake, beneath the trees, / Fluttering and dancing in the breeze.", "source": "William Wordsworth, Poems in Two Volumes (1807)", "read_time_secs": 12, "tags": "poetry,wordsworth,nature,daffodils", "likes": 8700, "description": "Wordsworth captures the profound joy found in nature's beauty and the power of memory to uplift the human spirit. Written after a walk with his sister Dorothy, the poem reflects the Romantic belief that nature offers emotional and spiritual nourishment that sustains us long after the moment has passed.", "poet": "William Wordsworth"},
    {"category_id": 25, "title": "Hope is the thing with feathers", "body": "Hope is the thing with feathers / That perches in the soul, / And sings the tune without the words, / And never stops at all.", "source": "Emily Dickinson, Complete Poems", "read_time_secs": 10, "tags": "poetry,dickinson,hope,inspiration", "likes": 9200, "description": "Dickinson's delicate and powerful metaphor portrays hope as a resilient bird that lives within the soul, singing tirelessly through storms and darkness. Despite life's harshest trials, hope asks nothing in return\u2014it simply persists, warm and unwavering.", "poet": "Emily Dickinson"},
    {"category_id": 25, "title": "Still I Rise", "body": "You may write me down in history / With your bitter, twisted lies, / You may trod me in the very dirt / But still, like dust, I'll rise. / ... / I am the dream and the hope of the slave. / I rise. / I rise. / I rise.", "source": "Maya Angelou, And Still I Rise (1978)", "read_time_secs": 15, "tags": "poetry,angelou,resilience,strength", "likes": 10500, "description": "Angelou's powerful anthem of resilience and self-worth confronts the legacy of racism and oppression with unshakable confidence. Written during the Civil Rights era, the poem pulses with a rhythmic defiance that transforms personal pain into collective triumph.", "poet": "Maya Angelou"},
    {"category_id": 25, "title": "The Guest House", "body": "This being human is a guest house. / Every morning a new arrival. / A joy, a depression, a meanness, / some momentary awareness comes / as an unexpected visitor. / Welcome and entertain them all! / ... / Be grateful for whoever comes, / because each has been sent / as a guide from beyond.", "source": "Rumi, translated by Coleman Barks", "read_time_secs": 15, "tags": "poetry,rumi,sufi,acceptance", "likes": 8800, "description": "Rumi's timeless wisdom invites us to welcome all emotions\u2014joy, sorrow, even shame\u2014as temporary guests that teach us something profound about being human. This 13th-century Sufi poem has found renewed relevance in modern mindfulness and acceptance-based therapies.", "poet": "Rumi"},
    {"category_id": 25, "title": "If\u2014", "body": "If you can keep your head when all about you / Are losing theirs and blaming it on you, / If you can trust yourself when all men doubt you, / But make allowance for their doubting too; / ... / Yours is the Earth and everything that's in it, / And\u2014which is more\u2014you'll be a Man, my son!", "source": "Rudyard Kipling, Rewards and Fairies (1910)", "read_time_secs": 20, "tags": "poetry,kipling,stoicism,virtue", "likes": 7600, "description": "Kipling's iconic poem distills Victorian stoic virtue into a father's advice to his son. Written during the British Empire's zenith, it lays out a blueprint for character: patience, integrity, humility, resilience, and the ability to dream without being ruled by dreams.", "poet": "Rudyard Kipling"},
    {"category_id": 25, "title": "Ozymandias", "body": "I met a traveller from an antique land, / Who said: 'Two vast and trunkless legs of stone / Stand in the desert. ... Near them, on the sand, / Half sunk, a shattered visage lies, whose frown, / And wrinkled lip, and sneer of cold command, / Tell that its sculptor well those passions read / ... / My name is Ozymandias, King of Kings; / Look on my Works, ye Mighty, and despair!' / Nothing beside remains. Round the decay / Of that colossal Wreck, boundless and bare / The lone and level sands stretch far away.", "source": "Percy Bysshe Shelley (1818)", "read_time_secs": 18, "tags": "poetry,shelley,power,transience", "likes": 8200, "description": "Shelley's masterful sonnet meditates on the transience of political power and the humbling passage of time. The shattered statue of Ozymandias (the Greek name for Ramesses II) stands as a haunting reminder that even the mightiest empires crumble to dust.", "poet": "Percy Bysshe Shelley"},
    # Shayari (category_id: 33) - Hindi/Urdu poetry from renowned poets
    {"category_id": 33, "title": "Hazaron Khwahishen Aisi", "body": "Hazaron khwahishen aisi, ke har khwahish pe dam nikle / Bahut nikle mere armaan, lekin phir bhi kam nikle / \u0939\u091c\u093e\u0930\u094b\u0902 \u0916\u094d\u0935\u093e\u0939\u093f\u0936\u0947\u0902 \u0910\u0938\u0940 \u0915\u093f \u0939\u0930 \u0916\u094d\u0935\u093e\u0939\u093f\u0936 \u092a\u0947 \u0926\u092e \u0928\u093f\u0915\u0932\u0947 / \u092c\u0939\u0941\u0924 \u0928\u093f\u0915\u0932\u0947 \u092e\u0947\u0930\u0947 \u0905\u0930\u092e\u093e\u0928 \u0932\u0947\u0915\u093f\u0928 \u092b\u093f\u0930 \u092d\u0940 \u0915\u092e \u0928\u093f\u0915\u0932\u0947", "source": "Mirza Ghalib, Diwan-e-Ghalib", "read_time_secs": 15, "tags": "shayari,ghalib,urdu,desire", "likes": 12500, "description": "Ghalib, the last great poet of the Mughal era, reflects on the endless nature of human desire. No matter how many wishes are fulfilled, the heart always yearns for more. This couplet captures the quintessential Ghalibian paradox: the ache of wanting and the incompleteness of having.", "poet": "Mirza Ghalib"},
    {"category_id": 33, "title": "Khudi Ko Kar Buland Itna", "body": "Khudi ko kar buland itna ke har taqdeer se pehle / Khuda bande se khud poochhe, bataa teri raza kya hai / \u0916\u0941\u0926\u0940 \u0915\u094b \u0915\u0930 \u092c\u0941\u0932\u0902\u0926 \u0907\u0924\u0928\u093e \u0915\u093f \u0939\u0930 \u0924\u0915\u0926\u0940\u0930 \u0938\u0947 \u092a\u0939\u0932\u0947 / \u0916\u0941\u0926\u093e \u092c\u0902\u0926\u0947 \u0938\u0947 \u0916\u0941\u0926 \u092a\u0942\u091b\u0947 \u092c\u0924\u093e \u0924\u0947\u0930\u0940 \u0930\u091c\u093e \u0915\u094d\u092f\u093e \u0939\u0948", "source": "Allama Iqbal, Bal-e-Jibreel", "read_time_secs": 12, "tags": "shayari,iqbal,urdu,self-empowerment", "likes": 11200, "description": "Iqbal's most celebrated couplet is an inspiring call to elevate one's selfhood (khudi) to such heights that even destiny consults you before deciding your fate. This cornerstone of Iqbal's philosophy empowers the individual to rise above circumstance through self-awareness and willpower.", "poet": "Allama Iqbal"},
    {"category_id": 33, "title": "Madhushala", "body": "Madhushala ek mahal hai jiska dwar hai pyasa / Jo bhi andar jaata hai, woh jaata hai udasa / \u092e\u0927\u0941\u0936\u093e\u0932\u093e \u090f\u0915 \u092e\u0939\u0932 \u0939\u0948 \u091c\u093f\u0938\u0915\u093e \u0926\u094d\u0935\u093e\u0930 \u0939\u0948 \u092a\u094d\u092f\u093e\u0938\u093e / \u091c\u094b \u092d\u0940 \u0905\u0902\u0926\u0930 \u091c\u093e\u0924\u093e \u0939\u0948 \u0935\u0939 \u091c\u093e\u0924\u093e \u0939\u0948 \u0909\u0926\u093e\u0938\u093e", "source": "Harivansh Rai Bachchan, Madhushala (1935)", "read_time_secs": 10, "tags": "shayari,bachchan,hindi,madhushala", "likes": 9500, "description": "Bachchan's iconic poem 'Madhushala' (The Tavern of Wine) is an allegory for life itself, where the intoxicating elixir of existence is offered to all who seek meaning. The tavern represents the world, the wine is knowledge or divine love, and the drinker is every soul on a quest for fulfillment.", "poet": "Harivansh Rai Bachchan"},
    {"category_id": 33, "title": "Mujh Se Pehli Si Mohabbat", "body": "Mujh se pehli si mohabbat, mere mehboob, na maang / Maine samjha tha ke tu hai to darakhshan hai hayat / \u092e\u0941\u091d \u0938\u0947 \u092a\u0939\u0932\u0940 \u0938\u0940 \u092e\u094b\u0939\u092c\u094d\u092c\u0924 \u092e\u0947\u0930\u0947 \u092e\u0939\u092c\u0942\u092c \u0928\u093e \u092e\u093e\u0902\u0917 / \u092e\u0948\u0902\u0928\u0947 \u0938\u092e\u091d\u093e \u0925\u093e \u0915\u0947 \u0924\u0942 \u0939\u0948 \u0924\u094b \u0926\u0930\u0916\u094d\u0936\u093e\u0901 \u0939\u0948 \u0939\u092f\u093e\u0924", "source": "Faiz Ahmed Faiz, Naqsh-e-Faryadi (1943)", "read_time_secs": 12, "tags": "shayari,faiz,urdu,love,longing", "likes": 10800, "description": "Faiz's heartbreakingly beautiful poem asks his beloved not to demand the same pure, untainted love as before\u2014because the poet has now seen the suffering of the world, the hunger, the injustice that cannot be ignored. A masterful blending of romantic and revolutionary poetry.", "poet": "Faiz Ahmed Faiz"},
    {"category_id": 33, "title": "Kabir Ke Dohe", "body": "Bada hua to kya hua, jaise ped khajoor / Paanthi ko chhaya nahi, phal laage ati door / \u092c\u0921\u093c\u093e \u0939\u0941\u0906 \u0924\u094b \u0915\u094d\u092f\u093e \u0939\u0941\u0906 \u091c\u0948\u0938\u0947 \u092a\u0947\u0921\u093c \u0916\u091c\u0942\u0930 / \u092a\u0928\u094d\u0925\u0940 \u0915\u094b \u091b\u093e\u092f\u093e \u0928\u0939\u0940\u0902 \u092b\u0932 \u0932\u093e\u0917\u0947 \u0905\u0924\u0940 \u0926\u0942\u0930", "source": "Sant Kabir, Kabir Ke Dohe", "read_time_secs": 10, "tags": "shayari,kabir,hindi,dohe,wisdom", "likes": 8500, "description": "Kabir's pointed observation uses the date palm as a metaphor: what good is stature if it offers no shade to the weary traveler? True greatness lies not in height or status, but in being useful and accessible to those in need.", "poet": "Sant Kabir"},
    {"category_id": 33, "title": "Gulzar Ki Nazm", "body": "Aankhon se utarta hua, dil mein utar jaata hai / Ek rishta aisa bhi hai, jo kuchh bhi nahi rehta / \u0906\u0902\u0916\u094b\u0902 \u0938\u0947 \u0909\u0924\u0930\u0924\u093e \u0939\u0941\u0906 \u0926\u093f\u0932 \u092e\u0947\u0902 \u0909\u0924\u0930 \u091c\u093e\u0924\u093e \u0939\u0948 / \u090f\u0915 \u0930\u093f\u0936\u094d\u0924\u093e \u0910\u0938\u093e \u092d\u0940 \u0939\u0948 \u091c\u094b \u0915\u0941\u091b \u092d\u0940 \u0928\u0939\u0940\u0902 \u0930\u0939\u0924\u093e", "source": "Gulzar, Raat Pashminey Ki", "read_time_secs": 10, "tags": "shayari,gulzar,hindi,relationships", "likes": 7800, "description": "Gulzar, one of India's most celebrated modern poets and lyricists, captures the paradox of a relationship that exists in the liminal space between presence and absence. A connection that descends from the eyes into the heart, yet somehow remains undefined and nameless.", "poet": "Gulzar"},
    {"category_id": 33, "title": "Dil Dhadakne Ka Sabab", "body": "Dil dhadakne ka sabab yaad aaya / Woh teri yaad thi, ab yaad aaya / \u0926\u093f\u0932 \u0927\u0921\u0915\u0928\u0947 \u0915\u093e \u0938\u092c\u092c \u092f\u093e\u0926 \u0906\u092f\u093e / \u0935\u094b \u0924\u0947\u0930\u0940 \u092f\u093e\u0926 \u0925\u0940 \u0905\u092c \u092f\u093e\u0926 \u0906\u092f\u093e", "source": "Mir Taqi Mir, Kulliyat-e-Mir", "read_time_secs": 10, "tags": "shayari,mir,urdu,longing,heart", "likes": 8200, "description": "Mir Taqi Mir, the 18th-century Urdu poet known as 'Khuda-e-Sukhan' (God of Poetry), captures the exquisite pain of rediscovering a forgotten grief. The heartbeat itself remembers what the conscious mind had buried.", "poet": "Mir Taqi Mir"},
    {"category_id": 33, "title": "Main Akela Aksar", "body": "Main akela aksar sochta hoon / Kabhi kabhi toh aisa lagta hai / Jaise koi aur jeeta hai mere andar / \u092e\u0948\u0902 \u0905\u0915\u0947\u0932\u093e \u0905\u0915\u094d\u0938\u0930 \u0938\u094b\u091a\u0924\u093e \u0939\u0942\u0901 / \u0915\u092d\u0940 \u0915\u092d\u0940 \u0924\u094b \u0910\u0938\u093e \u0932\u0917\u0924\u093e \u0939\u0948 / \u091c\u0948\u0938\u0947 \u0915\u094b\u0908 \u0914\u0930 \u091c\u0940\u0924\u093e \u0939\u0948 \u092e\u0947\u0930\u0947 \u0905\u0902\u0926\u0930", "source": "Dushyant Kumar, Saaye Mein Dhoop", "read_time_secs": 12, "tags": "shayari,dushyant,hindi,self-reflection", "likes": 7400, "description": "Dushyant Kumar's deeply introspective verse explores the uncanny feeling that there exists another self living within us\u2014a stranger who thinks, feels, and acts independently. A profound meditation on the multiplicity of identity.", "poet": "Dushyant Kumar"},
    {"category_id": 33, "title": "Rahim Ke Dohe", "body": "Rahiman dhaga prem ka, mat todo chatkaye / Toote se phir na jude, jude gaanth par jaaye / \u0930\u0939\u0940\u092e\u0928 \u0927\u093e\u0917\u093e \u092a\u094d\u0930\u0947\u092e \u0915\u093e \u092e\u0924 \u0924\u094b\u0921\u093c\u094b \u091a\u091f\u0915\u093e\u092f\u0947 / \u091f\u0942\u091f\u0947 \u0938\u0947 \u092b\u093f\u0930 \u0928 \u091c\u0941\u0921\u093c\u0947 \u091c\u0941\u0921\u093c\u0947 \u0917\u093e\u0902\u0920 \u092a\u0930 \u091c\u093e\u092f\u0947", "source": "Rahim, Rahim Ke Dohe", "read_time_secs": 8, "tags": "shayari,rahim,hindi,love,dohe", "likes": 7100, "description": "Rahim, the 16th-century poet and courtier in Emperor Akbar's court, offers timeless relationship wisdom through the metaphor of a thread. The thread of love must be handled with care\u2014once broken, it can be tied again, but the knot will always remain.", "poet": "Rahim"},
]

CREATE_CATEGORIES_TABLE = """
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    icon VARCHAR(100) DEFAULT '',
    color_hex VARCHAR(7) DEFAULT '',
    priority INTEGER DEFAULT 0,
    content_table_id INTEGER UNIQUE DEFAULT 0
);
"""

CREATE_PER_CATEGORY_TABLE = """
CREATE TABLE IF NOT EXISTS contents_{id} (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    description TEXT DEFAULT '',
    poet TEXT DEFAULT '',
    source TEXT DEFAULT '',
    source_url TEXT DEFAULT '',
    read_time_secs INTEGER DEFAULT 15,
    tags TEXT DEFAULT '',
    likes INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(title)
);
"""

CREATE_ARCHIVE_TABLE = """
CREATE TABLE IF NOT EXISTS archive_{id} (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    description TEXT DEFAULT '',
    poet TEXT DEFAULT '',
    source TEXT DEFAULT '',
    source_url TEXT DEFAULT '',
    read_time_secs INTEGER DEFAULT 15,
    tags TEXT DEFAULT '',
    likes INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    archived_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(title)
);
"""

UPDATE_CATEGORY_TABLE_ID = """
UPDATE categories SET content_table_id = %s WHERE name = %s AND content_table_id = 0;
"""

INSERT_CATEGORY = """
INSERT INTO categories (name, icon, color_hex, priority, l1_category)
VALUES (%s, %s, %s, %s, %s)
ON CONFLICT (name) DO UPDATE SET
    icon = EXCLUDED.icon,
    color_hex = EXCLUDED.color_hex,
    priority = EXCLUDED.priority,
    l1_category = EXCLUDED.l1_category;
"""

INSERT_CONTENT = """
INSERT INTO contents_{cat_id} (title, body, description, poet, source, read_time_secs, tags, likes)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
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
        for cat in CATEGORIES:
            db_id = cat["table_id"]
            cur.execute(CREATE_PER_CATEGORY_TABLE.format(id=db_id))
            cur.execute(CREATE_ARCHIVE_TABLE.format(id=db_id))
    print("✓ Tables ensured (categories + per-category content + archive)")


def seed_categories(conn):
    count = 0
    with conn.cursor() as cur:
        for cat in CATEGORIES:
            cur.execute(INSERT_CATEGORY, (cat["name"], cat["icon"], cat["color_hex"], cat["priority"], cat.get("l1_category", "Facts")))
            if cur.rowcount > 0:
                count += 1
                print(f"  Created category: {cat['name']}")
            # Set the stable content_table_id for this category
            cur.execute(UPDATE_CATEGORY_TABLE_ID, (cat["table_id"], cat["name"]))
    print(f"✓ Categories: {count} new, {len(CATEGORIES) - count} existing")


def seed_content(conn):
    # Content items use category_id values that match content_table_id directly
    count = 0
    with conn.cursor() as cur:
        for item in CONTENT:
            # Map CONTENT's category_id to category name, then to content_table_id
            # The CONTENT category_ids match the content_table_id values directly
            table_id = item["category_id"]
            sql = INSERT_CONTENT.format(cat_id=table_id)
            cur.execute(sql, (
                item["title"],
                item["body"],
                item.get("description", ""),
                item.get("poet", ""),
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
        cur.execute("DROP VIEW IF EXISTS contents CASCADE;")
        for cat in CATEGORIES:
            db_id = cat["table_id"]
            cur.execute(f"DROP TABLE IF EXISTS contents_{db_id} CASCADE;")
            cur.execute(f"DROP TABLE IF EXISTS archive_{db_id} CASCADE;")
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
