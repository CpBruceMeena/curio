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
    {"name": "English Poems", "icon": "auto_stories", "color_hex": "#f472b6", "priority": 13, "table_id": 25, "l1_category": "Poems"},
    {"name": "Movies", "icon": "movie", "color_hex": "#fb923c", "priority": 14, "table_id": 26},
    {"name": "Neuroscience", "icon": "microscope", "color_hex": "#a78bfa", "priority": 15, "table_id": 27},
    {"name": "Literature", "icon": "menu_book", "color_hex": "#fbbf24", "priority": 16, "table_id": 28},
    {"name": "Geography", "icon": "public", "color_hex": "#34d399", "priority": 17, "table_id": 29},
    {"name": "Music", "icon": "music_note", "color_hex": "#f472b6", "priority": 18, "table_id": 30},
    {"name": "Sports", "icon": "sports_soccer", "color_hex": "#fb923c", "priority": 19, "table_id": 31},
    {"name": "Food", "icon": "ramen_dining", "color_hex": "#f59e0b", "priority": 20, "table_id": 32},
    {"name": "Shayari", "icon": "edit_note", "color_hex": "#d946ef", "priority": 21, "table_id": 33, "l1_category": "Poems"},
    {"name": "Mixed Puzzles", "icon": "extension", "color_hex": "#f97316", "priority": 22, "table_id": 34, "l1_category": "Puzzles"},
    {"name": "Short Stories", "icon": "article", "color_hex": "#06b6d4", "priority": 23, "table_id": 35, "l1_category": "Short Stories"},

    # Poems subcategories
    {"name": "Hindi Poems", "icon": "auto_stories", "color_hex": "#f472b6", "priority": 24, "table_id": 36, "l1_category": "Poems"},

    {"name": "Classics", "icon": "menu_book", "color_hex": "#fbbf24", "priority": 32, "table_id": 44, "l1_category": "Poems"},
    {"name": "Modern", "icon": "brush", "color_hex": "#a78bfa", "priority": 33, "table_id": 45, "l1_category": "Poems"},

    # Puzzles subcategories
    {"name": "Sudoku", "icon": "grid_on", "color_hex": "#22d3ee", "priority": 25, "table_id": 37, "l1_category": "Puzzles"},
    {"name": "Math Puzzles", "icon": "calculate", "color_hex": "#fb923c", "priority": 26, "table_id": 38, "l1_category": "Puzzles"},
    {"name": "Logic Puzzles", "icon": "psychology", "color_hex": "#a78bfa", "priority": 27, "table_id": 39, "l1_category": "Puzzles"},
    {"name": "Word Puzzles", "icon": "abc", "color_hex": "#fbbf24", "priority": 28, "table_id": 40, "l1_category": "Puzzles"},

    # Short Stories subcategories
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
    # English Poems (category_id: 25) - English poetry from renowned poets
    {"category_id": 25, "title": "The Road Not Taken", "body": "Two roads diverged in a yellow wood,\nAnd sorry I could not travel both\nAnd be one traveler, long I stood\nAnd looked down one as far as I could\nTo where it bent in the undergrowth;\n...\nI took the one less traveled by,\nAnd that has made all the difference.", "source": "Robert Frost, Mountain Interval (1916)", "read_time_secs": 15, "tags": "poetry,frost,choices,life", "likes": 9800, "description": "One of Frost's most beloved poems explores the human experience of choice and its lasting impact on our lives. Often misinterpreted as a simple celebration of individualism, the poem's speaker acknowledges with a sigh that both paths were equally worn, and the choice itself\u2014not the path\u2014is what makes all the difference.", "poet": "Robert Frost"},
    {"category_id": 25, "title": "I Wandered Lonely as a Cloud", "body": "I wandered lonely as a cloud\nThat floats on high o'er vales and hills,\nWhen all at once I saw a crowd,\nA host, of golden daffodils;\nBeside the lake, beneath the trees,\nFluttering and dancing in the breeze.", "source": "William Wordsworth, Poems in Two Volumes (1807)", "read_time_secs": 12, "tags": "poetry,wordsworth,nature,daffodils", "likes": 8700, "description": "Wordsworth captures the profound joy found in nature's beauty and the power of memory to uplift the human spirit. Written after a walk with his sister Dorothy, the poem reflects the Romantic belief that nature offers emotional and spiritual nourishment that sustains us long after the moment has passed.", "poet": "William Wordsworth"},
    {"category_id": 25, "title": "Hope is the thing with feathers", "body": "Hope is the thing with feathers\nThat perches in the soul,\nAnd sings the tune without the words,\nAnd never stops at all.", "source": "Emily Dickinson, Complete Poems", "read_time_secs": 10, "tags": "poetry,dickinson,hope,inspiration", "likes": 9200, "description": "Dickinson's delicate and powerful metaphor portrays hope as a resilient bird that lives within the soul, singing tirelessly through storms and darkness. Despite life's harshest trials, hope asks nothing in return\u2014it simply persists, warm and unwavering.", "poet": "Emily Dickinson"},
    {"category_id": 25, "title": "Still I Rise", "body": "You may write me down in history\nWith your bitter, twisted lies,\nYou may trod me in the very dirt\nBut still, like dust, I'll rise.\n...\nI am the dream and the hope of the slave.\nI rise.\nI rise.\nI rise.", "source": "Maya Angelou, And Still I Rise (1978)", "read_time_secs": 15, "tags": "poetry,angelou,resilience,strength", "likes": 10500, "description": "Angelou's powerful anthem of resilience and self-worth confronts the legacy of racism and oppression with unshakable confidence. Written during the Civil Rights era, the poem pulses with a rhythmic defiance that transforms personal pain into collective triumph.", "poet": "Maya Angelou"},
    {"category_id": 25, "title": "The Guest House", "body": "This being human is a guest house.\nEvery morning a new arrival.\nA joy, a depression, a meanness,\nsome momentary awareness comes\nas an unexpected visitor.\nWelcome and entertain them all!\n...\nBe grateful for whoever comes,\nbecause each has been sent\nas a guide from beyond.", "source": "Rumi, translated by Coleman Barks", "read_time_secs": 15, "tags": "poetry,rumi,sufi,acceptance", "likes": 8800, "description": "Rumi's timeless wisdom invites us to welcome all emotions\u2014joy, sorrow, even shame\u2014as temporary guests that teach us something profound about being human. This 13th-century Sufi poem has found renewed relevance in modern mindfulness and acceptance-based therapies.", "poet": "Rumi"},
    {"category_id": 25, "title": "If\u2014", "body": "If you can keep your head when all about you\nAre losing theirs and blaming it on you,\nIf you can trust yourself when all men doubt you,\nBut make allowance for their doubting too;\n...\nYours is the Earth and everything that's in it,\nAnd\u2014which is more\u2014you'll be a Man, my son!", "source": "Rudyard Kipling, Rewards and Fairies (1910)", "read_time_secs": 20, "tags": "poetry,kipling,stoicism,virtue", "likes": 7600, "description": "Kipling's iconic poem distills Victorian stoic virtue into a father's advice to his son. Written during the British Empire's zenith, it lays out a blueprint for character: patience, integrity, humility, resilience, and the ability to dream without being ruled by dreams.", "poet": "Rudyard Kipling"},
    {"category_id": 25, "title": "Ozymandias", "body": "I met a traveller from an antique land,\nWho said: 'Two vast and trunkless legs of stone\nStand in the desert. ... Near them, on the sand,\nHalf sunk, a shattered visage lies, whose frown,\nAnd wrinkled lip, and sneer of cold command,\nTell that its sculptor well those passions read\n...\nMy name is Ozymandias, King of Kings;\nLook on my Works, ye Mighty, and despair!'\nNothing beside remains. Round the decay\nOf that colossal Wreck, boundless and bare\nThe lone and level sands stretch far away.", "source": "Percy Bysshe Shelley (1818)", "read_time_secs": 18, "tags": "poetry,shelley,power,transience", "likes": 8200, "description": "Shelley's masterful sonnet meditates on the transience of political power and the humbling passage of time. The shattered statue of Ozymandias (the Greek name for Ramesses II) stands as a haunting reminder that even the mightiest empires crumble to dust.", "poet": "Percy Bysshe Shelley"},
    # Shayari (category_id: 33) - Hindi/Urdu poetry
    {"category_id": 33, "title": "Hazaron Khwahishen Aisi", "body": "Hazaron khwahishen aisi, ke har khwahish pe dam nikle\nBahut nikle mere armaan, lekin phir bhi kam nikle\n\u0939\u091c\u093e\u0930\u094b\u0902 \u0916\u094d\u0935\u093e\u0939\u093f\u0936\u0947\u0902 \u0910\u0938\u0940 \u0915\u093f \u0939\u0930 \u0916\u094d\u0935\u093e\u0939\u093f\u0936 \u092a\u0947 \u0926\u092e \u0928\u093f\u0915\u0932\u0947\n\u092c\u0939\u0941\u0924 \u0928\u093f\u0915\u0932\u0947 \u092e\u0947\u0930\u0947 \u0905\u0930\u092e\u093e\u0928 \u0932\u0947\u0915\u093f\u0928 \u092b\u093f\u0930 \u092d\u0940 \u0915\u092e \u0928\u093f\u0915\u0932\u0947", "source": "Mirza Ghalib, Diwan-e-Ghalib", "read_time_secs": 15, "tags": "shayari,ghalib,urdu,desire", "likes": 12500, "description": "Ghalib, the last great poet of the Mughal era, reflects on the endless nature of human desire. No matter how many wishes are fulfilled, the heart always yearns for more.", "poet": "Mirza Ghalib"},
    {"category_id": 33, "title": "Khudi Ko Kar Buland Itna", "body": "Khudi ko kar buland itna ke har taqdeer se pehle\nKhuda bande se khud poochhe, bataa teri raza kya hai", "source": "Allama Iqbal, Bal-e-Jibreel", "read_time_secs": 12, "tags": "shayari,iqbal,urdu,self-empowerment", "likes": 11200, "description": "Iqbal's most celebrated couplet is an inspiring call to elevate one's selfhood (khudi) to such heights that even destiny consults you before deciding your fate.", "poet": "Allama Iqbal"},
    {"category_id": 33, "title": "Madhushala", "body": "Madhushala ek mahal hai jiska dwar hai pyasa\nJo bhi andar jaata hai, woh jaata hai udasa\n\u092e\u0927\u0941\u0936\u093e\u0932\u093e \u090f\u0915 \u092e\u0939\u0932 \u0939\u0948 \u091c\u093f\u0938\u0915\u093e \u0926\u094d\u0935\u093e\u0930 \u0939\u0948 \u092a\u094d\u092f\u093e\u0938\u093e\n\u091c\u094b \u092d\u0940 \u0905\u0902\u0926\u0930 \u091c\u093e\u0924\u093e \u0939\u0948 \u0935\u0939 \u091c\u093e\u0924\u093e \u0939\u0948 \u0909\u0926\u093e\u0938\u093e", "source": "Harivansh Rai Bachchan, Madhushala (1935)", "read_time_secs": 10, "tags": "shayari,bachchan,hindi,madhushala", "likes": 9500, "description": "Bachchan's iconic poem 'Madhushala' (The Tavern of Wine) is an allegory for life itself, where the intoxicating elixir of existence is offered to all who seek meaning.", "poet": "Harivansh Rai Bachchan"},
    {"category_id": 33, "title": "Mujh Se Pehli Si Mohabbat", "body": "Mujh se pehli si mohabbat, mere mehboob, na maang\nMaine samjha tha ke tu hai to darakhshan hai hayat", "source": "Faiz Ahmed Faiz, Naqsh-e-Faryadi (1943)", "read_time_secs": 12, "tags": "shayari,faiz,urdu,love,longing", "likes": 10800, "description": "Faiz's heartbreakingly beautiful poem asks his beloved not to demand the same pure love as before\u2014because the poet has now seen the suffering of the world.", "poet": "Faiz Ahmed Faiz"},
    {"category_id": 33, "title": "Kabir Ke Dohe", "body": "Bada hua to kya hua, jaise ped khajoor\nPaanthi ko chhaya nahi, phal laage ati door\nबड़ा हुआ तो क्या हुआ, जैसे पेड़ खजूर\nपंथी को छाया नहीं, फल लागे अति दूर", "source": "Sant Kabir, Kabir Ke Dohe", "read_time_secs": 10, "tags": "shayari,kabir,hindi,dohe,wisdom", "likes": 8500, "description": "Kabir's pointed observation uses the date palm as a metaphor: what good is stature if it offers no shade to the weary traveler?", "poet": "Sant Kabir"},
    {"category_id": 33, "title": "Gulzar Ki Nazm", "body": "Aankhon se utarta hua, dil mein utar jaata hai\nEk rishta aisa bhi hai, jo kuchh bhi nahi rehta\nआँखों से उतरता हुआ, दिल में उतर जाता है\nएक रिश्ता ऐसा भी है, जो कुछ भी नहीं रहता", "source": "Gulzar, Raat Pashminey Ki", "read_time_secs": 10, "tags": "shayari,gulzar,hindi,relationships", "likes": 7800, "description": "Gulzar captures the paradox of a relationship that exists in the liminal space between presence and absence.", "poet": "Gulzar"},
    {"category_id": 33, "title": "Dil Dhadakne Ka Sabab", "body": "Dil dhadakne ka sabab yaad aaya\nWoh teri yaad thi, ab yaad aaya", "source": "Mir Taqi Mir, Kulliyat-e-Mir", "read_time_secs": 10, "tags": "shayari,mir,urdu,longing,heart", "likes": 8200, "description": "Mir Taqi Mir captures the exquisite pain of rediscovering a forgotten grief. The heartbeat itself remembers what the conscious mind had buried.", "poet": "Mir Taqi Mir"},
    {"category_id": 33, "title": "Main Akela Aksar", "body": "Main akela aksar sochta hoon\nKabhi kabhi toh aisa lagta hai\nJaise koi aur jeeta hai mere andar\nमैं अकेला अक्सर सोचता हूँ\nकभी कभी तो ऐसा लगता है\nजैसे कोई और जीता है मेरे अंदर", "source": "Dushyant Kumar, Saaye Mein Dhoop", "read_time_secs": 12, "tags": "shayari,dushyant,hindi,self-reflection", "likes": 7400, "description": "Dushyant Kumar's deeply introspective verse explores the uncanny feeling that there exists another self living within us.", "poet": "Dushyant Kumar"},
    {"category_id": 33, "title": "Rahim Ke Dohe", "body": "Rahiman dhaga prem ka, mat todo chatkaye\nToote se phir na jude, jude gaanth par jaaye\nरहिमन धागा प्रेम का, मत तोड़ो चटकाये\nटूटे से फिर न जुड़े, जुड़े गाँठ पर जाये", "source": "Rahim, Rahim Ke Dohe", "read_time_secs": 8, "tags": "shayari,rahim,hindi,love,dohe", "likes": 7100, "description": "Rahim offers timeless relationship wisdom through the metaphor of a thread. Once broken, it can be tied again, but the knot will always remain.", "poet": "Rahim"},

    # Hindi Poems (table_id: 36) - Hindi poems
    {"category_id": 36, "title": "Wo Ladki", "body": "Wo ladki jise maine dekha tha ek baar\nBadalon ke us paar\nUs ladki ki yaad mein\nMaine likh dali ye kavita saari\nEk baar dekha tha use\nAur yaad aa gaya mujhe\nPoora ka poora sansar", "source": "Original Composition for Curio", "read_time_secs": 10, "tags": "hindi,kavita,poem", "likes": 1200, "description": "A Hindi poem about a fleeting glimpse that brings back memories of an entire world.", "poet": ""},
    {"category_id": 36, "title": "Aaj Phir Jeene Ki", "body": "Aaj phir jeene ki ichchha hui\nSubah ki dhoop mein\nChai ki pyali mein\nTumhari yaad mein\nAur is kavita ki panktiyon mein", "source": "Original Composition for Curio", "read_time_secs": 8, "tags": "hindi,kavita,morning,life", "likes": 980, "description": "A tender Hindi poem celebrating the simple joys of life.", "poet": ""},
    {"category_id": 36, "title": "Sapno Ka Shahar", "body": "Har gali mein ek kahani\nHar modh pe ek nishani\nYe shahar sapno ka bunkar hai\nHar subah ek nayi umeed\nHar shaam ek nayi tasveer\nYe shahar sapno ka bunkar hai", "source": "Original Composition for Curio", "read_time_secs": 10, "tags": "hindi,kavita,city,dreams", "likes": 850, "description": "A poetic tribute to a city of dreams, where every street holds a story.", "poet": ""},
    {"category_id": 36, "title": "Do Pal Ki Khushi", "body": "Do pal ki khushi ke liye\nSaari umr intezaar kiya\nMil gaye to laga\nBus yahi pal\nPoori zindagi ka saar hai", "source": "Original Composition for Curio", "read_time_secs": 8, "tags": "hindi,kavita,happiness,moments", "likes": 1100, "description": "A reflective poem about waiting a lifetime for moments of happiness.", "poet": ""},

    # Classic Fiction (table_id: 41) - Literary classics
    {"category_id": 41, "title": "The Tell-Tale Heart", "body": "True!-nervous-very, very dreadfully nervous I had been and am; but why will you say that I am mad? The disease had sharpened my senses-not destroyed-not dulled them. Above all was the sense of hearing acute. I heard all things in the heaven and in the earth. I heard many things in hell. How, then, am I mad? Hearken! and observe how healthily-how calmly I can tell you the whole story.", "source": "Edgar Allan Poe (1843)", "read_time_secs": 60, "tags": "classic,poe,horror,short-story", "likes": 3200, "description": "One of Poe's most gripping psychological horror stories. A masterclass in unreliable narration and building tension through rhythm and repetition.", "poet": ""},
    {"category_id": 41, "title": "The Gift of the Magi", "body": "One dollar and eighty-seven cents. That was all. And sixty cents of it was in pennies. Pennies saved one and two at a time by bulldozing the grocer and the vegetable man and the butcher. Three times Della counted it. One dollar and eighty-seven cents. And the next day would be Christmas.", "source": "O. Henry (1905)", "read_time_secs": 45, "tags": "classic,o-henry,christmas,love", "likes": 4100, "description": "O. Henry's timeless story of sacrifice and love. A heartwarming twist ending that has captivated readers for over a century.", "poet": ""},
    {"category_id": 41, "title": "The Open Boat", "body": "None of them knew the color of the sky. Their eyes glanced level, and were fastened upon the waves that swept toward them. These waves were of the hue of slate, save for the tops, which were of foaming white. The horizon narrowed and widened, and dipped and rose.", "source": "Stephen Crane (1897)", "read_time_secs": 55, "tags": "classic,crane,sea,survival", "likes": 2800, "description": "Crane's masterpiece of naturalism based on his own experience surviving a shipwreck. A profound meditation on solidarity and survival.", "poet": ""},

    # Micro Stories (table_id: 42) - Ultra-short fiction
    {"category_id": 42, "title": "The Last Question", "body": "The last human on Earth sat alone in a room. There was a knock at the door.", "source": "Original Micro Fiction", "read_time_secs": 5, "tags": "micro-fiction,sci-fi,twist", "likes": 5600, "description": "A six-word sci-fi story that turns the concept of 'alone' on its head.", "poet": ""},
    {"category_id": 42, "title": "The Paper Boat", "body": "She folded the letter into a paper boat and set it sailing down the gutter. Years later, her daughter found it washed up on a beach. Inside, the ink had blurred into a single word: 'Come home.'", "source": "Original Micro Fiction", "read_time_secs": 12, "tags": "micro-fiction,family,love,longing", "likes": 3400, "description": "A mother's desperate message travels across oceans and years to reach her estranged daughter.", "poet": ""},
    {"category_id": 42, "title": "The Last Library Book", "body": "The librarian stamped the due date: March 15, 2047. 'When do I need to return it?' asked the girl. The librarian smiled sadly. 'The library closes tomorrow. Forever. But this book is yours now. Take it. Read it. Remember that some things are worth preserving.'", "source": "Original Micro Fiction", "read_time_secs": 15, "tags": "micro-fiction,library,books,future", "likes": 2800, "description": "A poignant story about the end of physical libraries and the enduring power of books.", "poet": ""},
    {"category_id": 42, "title": "The Coffee Shop Regular", "body": "For three years, he ordered the same thing: black coffee, no sugar. One day, he left his notebook behind. She read one page: 'Day 1,095: Today I will speak to her.' She looked up. He was standing at the counter. 'You forgot this,' she said. 'I know,' he said.", "source": "Original Micro Fiction", "read_time_secs": 10, "tags": "micro-fiction,romance,cafe,confession", "likes": 4500, "description": "A sweet micro-story about a three-year coffee shop crush that finally breaks the surface.", "poet": ""},

    # Serialized Stories (table_id: 43) - Multi-part tales
    {"category_id": 43, "title": "The Lighthouse Keeper (Part 1: The Storm)", "body": "PART 1: THE STORM\n\nThe lighthouse had stood for two hundred years, but never had it faced a storm like this. Elias tightened the last bolt on the lantern mechanism and steadied himself against the tower's shudder. Below, waves crashed against the granite foundation with a fury that seemed almost personal. His son, Leo, handed him a fresh cup of coffee. 'Will it hold?' Leo shouted over the wind. Elias looked at the ancient lens\u2014the same one his father had polished, and his grandfather before that. 'It has to,' he said. 'There's a ship out there.'\n\n[Continued in Part 2]", "source": "Original Serial Fiction for Curio", "read_time_secs": 30, "tags": "serial,lighthouse,storm,drama", "likes": 1800, "description": "Part 1 of a gripping serial about a lighthouse keeper fighting to keep the light burning during a historic storm.", "poet": ""},
    {"category_id": 43, "title": "The Lighthouse Keeper (Part 2: The Signal)", "body": "PART 2: THE SIGNAL\n\nThe ship's lights had vanished. Elias pressed his face against the storm-streaked glass, searching the churning darkness. 'I can't see it anymore,' Leo said, his voice tight with fear. Elias began to crank the emergency reflector into position. It was a desperate measure\u2014a signal flare system that hadn't been used since the war. 'But father,' Leo protested, 'that could set the whole tower alight!' Elias smiled grimly. 'Then at least they'll see us burn.'\n\n[Finale in Part 3]", "source": "Original Serial Fiction for Curio", "read_time_secs": 25, "tags": "serial,lighthouse,signal,sacrifice", "likes": 1600, "description": "Part 2 raises the stakes as Elias prepares to risk everything to save the ship.", "poet": ""},
    {"category_id": 43, "title": "The Lighthouse Keeper (Part 3: The Rescue)", "body": "PART 3: THE RESCUE\n\nThe flare ignited the sky. For one brilliant moment, the entire coast was illuminated\u2014the jagged rocks, the churning surf, and the ship, perilously close to the reef. 'They see us!' Leo shouted. Below, a lifeboat was being lowered. As dawn broke and the storm subsided, the ship anchored safely in the harbor. Elias looked at his son. 'We kept the light burning. That's all we've ever done.'\n\nTHE END", "source": "Original Serial Fiction for Curio", "read_time_secs": 35, "tags": "serial,lighthouse,rescue,conclusion", "likes": 2000, "description": "The thrilling conclusion! The desperate flare works, the ship is saved, and Elias passes the legacy to his son.", "poet": ""},

    # Classics (table_id: 44) - Classic English poems
    {"category_id": 44, "title": "Sonnet 18", "body": "Shall I compare thee to a summer's day? Thou art more lovely and more temperate. Rough winds do shake the darling buds of May, And summer's lease hath all too short a date. But thy eternal summer shall not fade, So long as men can breathe or eyes can see, So long lives this, and this gives life to thee.", "source": "William Shakespeare (1609)", "read_time_secs": 15, "tags": "classic,shakespeare,sonnet,poetry", "likes": 11500, "description": "Shakespeare's most famous sonnet immortalizes the beloved through poetry itself.", "poet": "William Shakespeare"},
    {"category_id": 44, "title": "Kubla Khan", "body": "In Xanadu did Kubla Khan A stately pleasure-dome decree. Where Alph, the sacred river, ran Through caverns measureless to man Down to a sunless sea.", "source": "Samuel Taylor Coleridge (1816)", "read_time_secs": 18, "tags": "classic,coleridge,romantic,visionary", "likes": 7200, "description": "Coleridge's visionary fragment creates an otherworldly atmosphere.", "poet": "Samuel Taylor Coleridge"},
    {"category_id": 44, "title": "Dover Beach", "body": "The sea is calm tonight. The tide is full, the moon lies fair Upon the straits. Ah, love, let us be true To one another! for the world hath really neither joy, nor love, nor light, Nor certitude, nor peace, nor help for pain.", "source": "Matthew Arnold (1867)", "read_time_secs": 15, "tags": "classic,arnold,victorian,sea,faith", "likes": 6800, "description": "Arnold's melancholic masterpiece captures the Victorian crisis of faith.", "poet": "Matthew Arnold"},

    # Modern (table_id: 45) - Modern English poems
    {"category_id": 45, "title": "The Love Song of J. Alfred Prufrock", "body": "Let us go then, you and I, When the evening is spread out against the sky Like a patient etherized upon a table. Do I dare Disturb the universe?", "source": "T.S. Eliot (1915)", "read_time_secs": 25, "tags": "modern,eliot,modernist,anxiety", "likes": 8500, "description": "Eliot's revolutionary poem captures the existential anxiety of the 20th century.", "poet": "T.S. Eliot"},
    {"category_id": 45, "title": "Do Not Go Gentle Into That Good Night", "body": "Do not go gentle into that good night. Old age should burn and rave at close of day. Rage, rage against the dying of the light.", "source": "Dylan Thomas (1951)", "read_time_secs": 12, "tags": "modern,thomas,death,defiance", "likes": 10500, "description": "Thomas's powerful villanelle transforms grief into life-affirming resistance.", "poet": "Dylan Thomas"},
    {"category_id": 45, "title": "The Waste Land (Excerpt)", "body": "April is the cruellest month, breeding Lilacs out of the dead land. I will show you fear in a handful of dust. These fragments I have shored against my ruins.", "source": "T.S. Eliot (1922)", "read_time_secs": 20, "tags": "modern,eliot,waste-land,fragmentation", "likes": 7800, "description": "The defining poem of modernist literature.", "poet": "T.S. Eliot"},

    # Mixed Puzzles (table_id: 34) - Puzzle/riddle content for the feed
    {"category_id": 34, "title": "The Missing Dollar Riddle", "body": "Three friends pay $30 for a hotel room ($10 each). Later the clerk realizes the room is only $25 and gives $5 to the bellboy to return. The bellboy keeps $2 and gives $1 back to each friend. Now each friend paid $9 (3 x $9 = $27), plus the $2 the bellboy kept = $29. Where did the missing dollar go?", "source": "Classic Math Riddle", "read_time_secs": 20, "tags": "puzzle,riddle,math,logic", "likes": 450, "description": "", "poet": ""},
    {"category_id": 34, "title": "What Comes Next? Sequence Puzzle", "body": "What number comes next in this sequence? 2, 6, 18, 54, ?", "source": "Math Patterns", "read_time_secs": 12, "tags": "puzzle,sequence,math,pattern", "likes": 320, "description": "", "poet": ""},
    {"category_id": 34, "title": "The Two Doors Riddle", "body": "You're in a room with two doors. One leads to treasure, the other to certain doom. Two guards stand before the doors. One always tells the truth, the other always lies. You may ask ONE question to ONE guard to find the treasure door. What do you ask?", "source": "Classic Logic Puzzle", "read_time_secs": 25, "tags": "puzzle,logic,riddle,reasoning", "likes": 680, "description": "", "poet": ""},
    {"category_id": 34, "title": "Water Jug Problem", "body": "You have a 5-gallon jug and a 3-gallon jug. How can you measure exactly 4 gallons of water?", "source": "Die Hard Water Puzzle", "read_time_secs": 20, "tags": "puzzle,water,logic,measure", "likes": 390, "description": "", "poet": ""},
    {"category_id": 34, "title": "The River Crossing Puzzle", "body": "A farmer needs to cross a river with a wolf, a goat, and a cabbage. His boat can only carry him and one item at a time. If left alone, the wolf eats the goat, and the goat eats the cabbage. How does he get everything across safely?", "source": "Classic River Crossing Puzzle", "read_time_secs": 18, "tags": "puzzle,logic,river,reasoning", "likes": 520, "description": "", "poet": ""},
    {"category_id": 34, "title": "Clock Angle Puzzle", "body": "What is the angle between the hour hand and minute hand at 3:15?", "source": "Clock Geometry Puzzle", "read_time_secs": 12, "tags": "puzzle,clock,angle,geometry", "likes": 340, "description": "", "poet": ""},
    {"category_id": 34, "title": "Lateral Thinking: The Man in the Elevator", "body": "A man lives on the 10th floor. Every morning he takes the elevator down to the ground floor and goes to work. When he returns in the evening, he takes the elevator up to the 7th floor and walks up the remaining 3 flights. Why?", "source": "Lateral Thinking Puzzle", "read_time_secs": 10, "tags": "puzzle,lateral,thinking,elevator", "likes": 610, "description": "", "poet": ""},
    {"category_id": 34, "title": "Magic Square: Fill the Grid", "body": "Arrange the numbers 1 through 9 in a 3x3 grid so that each row, column, and diagonal sums to 15.", "source": "Magic Square Puzzle", "read_time_secs": 15, "tags": "puzzle,magic-square,grid,logic", "likes": 270, "description": "", "poet": ""},
    {"category_id": 34, "title": "Number Pattern: Find the Odd One Out", "body": "Which number does not belong? 3, 5, 7, 9, 11, 13, 15", "source": "Number Theory Puzzle", "read_time_secs": 8, "tags": "puzzle,numbers,prime,pattern", "likes": 180, "description": "", "poet": ""},
    {"category_id": 34, "title": "The Mango Math Problem", "body": "A merchant bought 100 mangoes. He sold 40% of them at a 20% profit and the rest at a 10% loss. What was his overall percentage profit or loss?", "source": "Business Math Puzzle", "read_time_secs": 15, "tags": "puzzle,profit,percentage,math", "likes": 160, "description": "", "poet": ""},
    {"category_id": 34, "title": "Matchstick Triangle Puzzle", "body": "Move exactly 2 matchsticks to make 4 equal triangles from 4 small triangles arranged in a larger triangle.", "source": "Matchstick Puzzle", "read_time_secs": 15, "tags": "puzzle,matchstick,geometry,spatial", "likes": 210, "description": "", "poet": ""},
    {"category_id": 34, "title": "Age Puzzle: How Old Are They?", "body": "A father is 4 times as old as his son. In 20 years, the father will be twice as old as his son. How old are they now?", "source": "Algebra Puzzle", "read_time_secs": 15, "tags": "puzzle,age,algebra,math", "likes": 280, "description": "", "poet": ""},
]

CREATE_CATEGORIES_TABLE = """
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    icon VARCHAR(100) DEFAULT '',
    color_hex VARCHAR(7) DEFAULT '',
    priority INTEGER DEFAULT 0,
    content_table_id INTEGER UNIQUE DEFAULT 0,
    l1_category VARCHAR(100) DEFAULT 'Facts'
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
    print("\u2713 Tables ensured (categories + per-category content + archive)")


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
    print(f"\u2713 Categories: {count} new, {len(CATEGORIES) - count} existing")


def seed_content(conn):
    count = 0
    with conn.cursor() as cur:
        for item in CONTENT:
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
    print(f"\u2713 Content: {count} new, {len(CONTENT) - count} existing")

# Puzzles seed data
PUZZLES = [
    # Sudoku (table_id 37)
    {"puzzle_type": "sudoku", "category_name": "Sudoku", "title": "Easy Sudoku #1",
     "question": "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
     "answer": "534678912672195348198342567859761423426853791713924856961537284287419635345286179",
     "answer_type": "text", "hint": "Focus on filling the top-left 3x3 box first. Row 1 has 5, 3 and 7.",
     "explanation": "Great work! This is a classic easy Sudoku.",
     "difficulty": 1, "likes": 120},
    {"puzzle_type": "sudoku", "category_name": "Sudoku", "title": "Medium Sudoku #1",
     "question": "020000600580107000000000004300200010000040000090008007600000000000409035009000080",
     "answer": "124853697586197243937624514348275161715349826892468357673582471261459335449716882",
     "answer_type": "text", "hint": "Look at column 4 - only a few numbers can fit.",
     "explanation": "Well solved! Medium puzzles require looking at intersections.",
     "difficulty": 2, "likes": 85},

    # Math Puzzles (category "Math Puzzles", table_id 38)
    {"puzzle_type": "math", "category_name": "Math Puzzles", "title": "The Number Trick",
     "question": "Think of a number. Add 5. Double it. Subtract 4. Divide by 2. Subtract the original number. What's the result?",
     "answer": "3", "answer_type": "number",
     "hint": "Try it with any number - you'll always get the same result!",
     "explanation": "Let the number be n. ((n+5)\u00d72-4)/2 - n = (2n+10-4)/2 - n = (2n+6)/2 - n = n+3-n = 3",
     "difficulty": 1, "likes": 145},
    {"puzzle_type": "math", "category_name": "Math Puzzles", "title": "The Age Riddle",
     "question": "A father is four times as old as his daughter. In 20 years, he will be twice as old as she will be. How old is the father now?",
     "answer": "40", "answer_type": "number",
     "hint": "Let daughter's age be x. Father = 4x. In 20 years: 4x+20 = 2(x+20)",
     "explanation": "Daughter = 10, Father = 40. In 20 years: Daughter = 30, Father = 60.",
     "difficulty": 2, "likes": 210},
    {"puzzle_type": "math", "category_name": "Math Puzzles", "title": "The Missing Number",
     "question": "What number should replace the question mark?\n2, 6, 18, 54, ?",
     "answer": "162", "answer_type": "number",
     "hint": "Look at the ratio between consecutive numbers.",
     "explanation": "Each term is multiplied by 3: 2\u00d73=6, 6\u00d73=18, 18\u00d73=54, 54\u00d73=162",
     "difficulty": 1, "likes": 180},
    {"puzzle_type": "math", "category_name": "Math Puzzles", "title": "The Clock Angle",
     "question": "What's the angle between the hour and minute hands at 3:15? (Answer in degrees)",
     "answer": "7.5", "answer_type": "number",
     "hint": "The hour hand moves 0.5 degrees per minute, not just staying at 3!",
     "explanation": "At 3:15, minute hand is at 3. Hour hand is 1/4 of the way to 4. 30\u00b0\u00f74 = 7.5\u00b0",
     "difficulty": 2, "likes": 155},

    # Logic Puzzles (category "Logic Puzzles", table_id 39)
    {"puzzle_type": "logic", "category_name": "Logic Puzzles", "title": "The Two Guards",
     "question": "Two doors. One leads to treasure, the other to doom. One guard tells truth, one lies. Ask ONE question to ONE guard. What question guarantees you find the treasure?",
     "answer": "What would the other guard say is the treasure door?", "answer_type": "text",
     "hint": "Think about what BOTH guards would answer. They point to the same door - the WRONG one.",
     "explanation": "Ask 'What would the other guard say?' The liar tells the opposite, the truth-teller repeats the lie. Both point to the wrong door - choose the OTHER door!",
     "difficulty": 3, "likes": 320},
    {"puzzle_type": "logic", "category_name": "Logic Puzzles", "title": "The River Crossing",
     "question": "A farmer must cross a river with a wolf, goat, and cabbage. Boat carries him and one item. Wolf eats goat, goat eats cabbage if left alone. What's the minimum number of crossings?",
     "answer": "7", "answer_type": "number",
     "hint": "The goat must come back on the 4th crossing.",
     "explanation": "1. Take goat across. 2. Return. 3. Take wolf. 4. Bring goat BACK. 5. Take cabbage. 6. Return. 7. Take goat. Total: 7.",
     "difficulty": 3, "likes": 280},
    {"puzzle_type": "logic", "category_name": "Logic Puzzles", "title": "The Light Switches",
     "question": "Three switches control three bulbs in another room. Flip switches any way, enter the bulb room ONCE. How to determine which switch controls which bulb?",
     "answer": "Turn on switch 1, wait 5 min, turn it off. Turn on switch 2. Enter. Bulb ON = switch 2. Warm OFF = switch 1. Cold OFF = switch 3.", "answer_type": "text",
     "hint": "What property of a bulb changes after being on?",
     "explanation": "Use HEAT! ON bulb = switch 2. OFF but WARM = switch 1. OFF and COLD = switch 3.",
     "difficulty": 4, "likes": 450},

    # Word Puzzles (category "Word Puzzles", table_id 40)
    {"puzzle_type": "word", "category_name": "Word Puzzles", "title": "What Am I?",
     "question": "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
     "answer": "echo", "answer_type": "text",
     "hint": "Think about what happens when you shout in a mountain valley.",
     "explanation": "An echo is a reflection of sound that 'speaks back' to you. It needs air/wind to carry sound waves.",
     "difficulty": 1, "likes": 230},
    {"puzzle_type": "word", "category_name": "Word Puzzles", "title": "The Anagram",
     "question": "Rearrange the letters of 'LISTEN' to form a new word. What is it?",
     "answer": "silent", "answer_type": "text",
     "hint": "What you should be doing right now to solve this!",
     "explanation": "'LISTEN' rearranged forms 'SILENT'. These are called anagrams.",
     "difficulty": 1, "likes": 170},
]

PUZZLE_INSERT = """
INSERT INTO puzzles (puzzle_type, category_id, title, question, answer, answer_type, hint, explanation, difficulty, likes)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
ON CONFLICT DO NOTHING;
"""


def seed_puzzles(conn):
    """Insert puzzle data. Looks up category IDs by name."""
    cat_id_map = {}
    with conn.cursor() as cur:
        cur.execute("SELECT id, name FROM categories WHERE name IN ('Sudoku', 'Math Puzzles', 'Logic Puzzles', 'Word Puzzles')")
        for row in cur.fetchall():
            cat_id_map[row[1]] = row[0]

    if not cat_id_map:
        print("  \u26a0 Puzzle categories not found in DB. Run category seeding first.")
        return

    count = 0
    with conn.cursor() as cur:
        for p in PUZZLES:
            cat_name = p["category_name"]
            cat_id = cat_id_map.get(cat_name)
            if cat_id is None:
                print(f"  \u26a0 Category '{cat_name}' not found, skipping puzzle")
                continue
            cur.execute(PUZZLE_INSERT, (
                p["puzzle_type"], cat_id, p["title"], p["question"], p["answer"],
                p["answer_type"], p["hint"], p["explanation"], p["difficulty"], p["likes"]
            ))
            if cur.rowcount > 0:
                count += 1
    print(f"\u2713 Puzzles: {count} new")


def verify_puzzles(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM puzzles;")
        cnt = cur.fetchone()[0]
    print(f"  Total puzzles in DB: {cnt}")


def rebuild_view(conn):
    """Rebuild the contents VIEW to include all per-category tables."""
    with conn.cursor() as cur:
        cur.execute("DROP VIEW IF EXISTS contents CASCADE;")
        cur.execute("SELECT id, name, COALESCE(NULLIF(content_table_id, 0), id) as tbl_id FROM categories ORDER BY id;")
        categories = cur.fetchall()
        if not categories:
            print("  \u26a0 No categories found, cannot rebuild VIEW")
            return
        parts = []
        for cat_id, cat_name, tbl_id in categories:
            safe_name = cat_name.replace("'", "''")
            parts.append(
                "    SELECT\n"
                "        (c" + str(cat_id) + ".id + " + str(cat_id) + "0000000)::integer AS id,\n"
                "        " + str(cat_id) + " AS category_id,\n"
                "        '" + safe_name + "'::varchar(255) AS category_name,\n"
                "        c" + str(cat_id) + ".title, c" + str(cat_id) + ".body, c" + str(cat_id) + ".description, c" + str(cat_id) + ".poet,"
                " c" + str(cat_id) + ".source, c" + str(cat_id) + ".source_url,\n"
                "        c" + str(cat_id) + ".read_time_secs, c" + str(cat_id) + ".tags, c" + str(cat_id) + ".likes, c" + str(cat_id) + ".created_at\n"
                "    FROM contents_" + str(tbl_id) + " c" + str(cat_id)
            )
        sql = "CREATE OR REPLACE VIEW contents AS\n" + "\nUNION ALL\n".join(parts) + ";"
        cur.execute(sql)
        cur.execute("SELECT COUNT(*) FROM contents;")
        total = cur.fetchone()[0]
    print(f"\u2713 VIEW rebuilt: {len(categories)} categories, {total} content items")


def reset_tables(conn):
    with conn.cursor() as cur:
        cur.execute("DROP VIEW IF EXISTS contents CASCADE;")
        for cat in CATEGORIES:
            db_id = cat["table_id"]
            cur.execute(f"DROP TABLE IF EXISTS contents_{db_id} CASCADE;")
            cur.execute(f"DROP TABLE IF EXISTS archive_{db_id} CASCADE;")
        cur.execute("DROP TABLE IF EXISTS categories CASCADE;")
    print("\u2713 Tables dropped")
    migrate(conn)


def verify(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM categories;")
        cat_count = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM contents;")
        con_count = cur.fetchone()[0]
    print(f"\n\u2713 Verification: {cat_count} categories, {con_count} content items in database")


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

    print("Rebuilding contents VIEW...")
    rebuild_view(conn)

    verify(conn)

    print("\nSeeding puzzles...")
    seed_puzzles(conn)
    verify_puzzles(conn)

    conn.close()
    print("\nSeed complete!")


if __name__ == "__main__":
    main()
