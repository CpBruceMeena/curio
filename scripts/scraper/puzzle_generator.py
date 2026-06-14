"""
Programmatic Puzzle Generator for Curio

Generates puzzles across 4 categories:
  - Sudoku (easy/medium/hard)
  - Math Puzzles (sequences, algebra, number tricks, geometry)
  - Logic Puzzles (truth-teller/liar, river crossing, lateral thinking)
  - Word Puzzles (anagrams, word riddles, palindromes)

Each generator returns dicts matching the `puzzles` table schema.
"""

import random
from typing import Any


# ─── Helpers ────────────────────────────────────────────────────────────

CATEGORY_IDS = {
    "Sudoku": 27,
    "Math Puzzles": 28,
    "Logic Puzzles": 29,
    "Word Puzzles": 30,
}


def _pick(seq: list) -> Any:
    return random.choice(seq)


def _age_problem() -> dict:
    """Generate an age word problem with unique ages."""
    attempts = 0
    while attempts < 20:
        attempts += 1
        # Person A is `ratio` times as old as Person B
        # In `future` years, A is `future_ratio` times as old as B
        ratio = random.randint(2, 5)
        future_ratio = random.randint(2, 3)  # must be < ratio
        if future_ratio >= ratio:
            continue
        # Solve: A = ratio * B; A + future = future_ratio * (B + future)
        # ratio*B + future = future_ratio*B + future_ratio*future
        # B*(ratio - future_ratio) = future*(future_ratio - 1)
        # B = future*(future_ratio - 1) / (ratio - future_ratio)
        future = random.randint(5, 25)
        numerator = future * (future_ratio - 1)
        denominator = ratio - future_ratio
        if numerator % denominator != 0:
            continue
        b = numerator // denominator
        a = ratio * b
        if b <= 0 or a <= 0 or a > 150 or b > 100:
            continue
        person_a = _pick(["father", "mother", "grandfather", "aunt", "uncle"])
        person_b = _pick(["daughter", "son", "nephew", "niece", "grandson", "granddaughter"])
        if b == a:
            continue
        if a == future_ratio * (b + future):
            continue  # skip trivial checks
        question = (
            f"A {person_a} is {ratio} times as old as {_pick(['their', 'his', 'her'])} {person_b}. "
            f"In {future} years, the {person_a} will be {future_ratio} times as old as the {person_b}. "
            f"How old is the {person_a} now?"
        )
        return {
            "title": f"Age Puzzle — {person_a.title()} and {person_b.title()}",
            "question": question,
            "answer": str(a),
            "answer_type": "number",
            "hint": f"Let {person_b}'s age = x. Then {person_a} = {ratio}x. In {future} years: {ratio}x + {future} = {future_ratio}(x + {future}).",
            "explanation": f"Let {person_b}'s age be x. {person_a} = {ratio}x. In {future} years: {ratio}x + {future} = {future_ratio}(x + {future}). Solve: {ratio}x + {future} = {future_ratio}x + {future * future_ratio} → ({ratio - future_ratio})x = {future * future_ratio - future} → x = {b}. So {person_b} = {b}, {person_a} = {a}.",
            "difficulty": 2,
            "likes": random.randint(100, 300),
        }
    return _sequence_puzzle()  # fallback


def _sequence_puzzle() -> dict:
    """Generate a number sequence puzzle."""
    seq_type = _pick(["arithmetic", "geometric", "fibonacci-like", "alternating"])
    terms = []
    answer = 0
    n_terms = random.randint(4, 6)

    if seq_type == "arithmetic":
        start = random.randint(-20, 50)
        step = random.choice([2, 3, 5, 10, -2, -3, -5, 1, 4, 6, 7, 8, 9, -1, -4])
        terms = [start + step * i for i in range(n_terms)]
        answer = start + step * n_terms
    elif seq_type == "geometric":
        start = random.randint(1, 8)
        factor = random.choice([2, 3, 4, 5, -2, -3])
        terms = [start * (factor ** i) for i in range(n_terms)]
        answer = start * (factor ** n_terms)
    elif seq_type == "fibonacci-like":
        a = random.randint(1, 10)
        b = random.randint(1, 15)
        terms = [a, b]
        for i in range(n_terms - 2):
            terms.append(terms[-1] + terms[-2])
        answer = terms[-1] + terms[-2] if n_terms >= 3 else 0
        # Rebuild with n_terms
        terms = [a, b]
        for i in range(n_terms - 2):
            terms.append(terms[-1] + terms[-2])
        answer = terms[-1] + terms[-2]
    elif seq_type == "alternating":
        # Two interleaved sequences
        start1 = random.randint(1, 10)
        step1 = random.choice([1, 2, 3, 5])
        start2 = random.randint(1, 10)
        step2 = random.choice([1, 2, 3, 5])
        seq1 = [start1 + step1 * i for i in range(n_terms)]
        seq2 = [start2 + step2 * i for i in range(n_terms)]
        terms = []
        for i in range(n_terms):
            terms.append(seq1[i])
            if len(terms) < n_terms:
                terms.append(seq2[i])
        terms = terms[:n_terms]
        # Next is the next from the sequence that would come next
        if len(terms) % 2 == 0:
            answer = seq1[len(terms) // 2]
        else:
            answer = seq2[len(terms) // 2]

    if len(terms) < 4 or all(t == terms[0] for t in terms):
        return _sequence_puzzle()

    question = f"What number comes next in this sequence?\n{', '.join(str(t) for t in terms)}, ?"

    # Build explanation based on the sequence type
    if seq_type == "arithmetic":
        explanation = f"Each term increases by {step}. {terms[-1]} + {step} = {answer}."
    elif seq_type == "geometric":
        explanation = f"Each term is multiplied by {factor}. {terms[-1]} × {factor} = {answer}."
    elif seq_type == "fibonacci-like":
        explanation = f"Each term is the sum of the two preceding terms. {terms[-1]} + {terms[-2]} = {answer}."
    else:
        explanation = f"The sequence is two interleaved sequences. {answer} is the next in the first/second pattern."

    difficulty = 1 if seq_type in ("arithmetic", "geometric") else 2

    return {
        "title": f"Sequence Puzzle — {seq_type.title()} Pattern",
        "question": question,
        "answer": str(answer),
        "answer_type": "number",
        "hint": f"Look at the difference (or ratio) between consecutive terms.",
        "explanation": explanation,
        "difficulty": difficulty,
        "likes": random.randint(80, 250),
    }


def _number_trick() -> dict:
    """Generate a 'think of a number' trick puzzle."""
    # Always produces result 3
    ops = [
        ("Add 5", "Double it", "Subtract 6", "Divide by 2", "Subtract the original number"),
        ("Multiply by 2", "Add 8", "Divide by 2", "Subtract the original number"),
        ("Add 7", "Triple it", "Subtract 3", "Divide by 3", "Subtract the original number"),
    ]
    results = [3, 4, 6]
    idx = random.randrange(len(ops))
    steps = ops[idx]
    result = results[idx]

    step_text = ". ".join(f"{i+1}. {s}" for i, s in enumerate(steps))
    question = f"Think of a number. {step_text}. What's the result?"

    # Derive algebraic proof
    proof_parts = ["n"]
    for s in steps:
        current = proof_parts[-1]
        if s == "Subtract the original number":
            proof_parts.append(f"{current} - n")
        elif s.startswith("Add"):
            val = int(s.split()[-1])
            proof_parts.append(f"{current} + {val}")
        elif s.startswith("Subtract"):
            val = int(s.split()[-1])
            proof_parts.append(f"{current} - {val}")
        elif s in ("Multiply by 2", "Double it"):
            proof_parts.append(f"2 \u00d7 ({current})")
        elif s == "Triple it":
            proof_parts.append(f"3 \u00d7 ({current})")
        elif s.startswith("Divide"):
            val = s.split()[-1]  # "by 2" -> "2", "by 3" -> "3"
            proof_parts.append(f"({current}) \u00f7 {val}")

    explanation = f"Let n be your number. After the steps: {proof_parts[-1]}. This always simplifies to {result}, no matter what n is!"

    return {
        "title": f"Number Trick — Always {result}!",
        "question": question,
        "answer": str(result),
        "answer_type": "number",
        "hint": "Try it with any number — you'll always get the same result!",
        "explanation": explanation,
        "difficulty": 1,
        "likes": random.randint(120, 300),
    }


def _clock_angle() -> dict:
    """Generate a clock angle puzzle."""
    hour = random.randint(1, 11)
    minute = random.choice([0, 15, 20, 30, 40, 45])
    # Calculate angle
    hour_angle = (hour % 12) * 30 + minute * 0.5
    minute_angle = minute * 6
    angle = abs(hour_angle - minute_angle)
    angle = min(angle, 360 - angle)

    if angle == int(angle):
        answer = str(int(angle))
    else:
        answer = f"{angle:.1f}"

    return {
        "title": f"Clock Angle Puzzle — {hour}:{minute:02d}",
        "question": f"What's the angle between the hour hand and minute hand at {hour}:{minute:02d}? (Answer in degrees)",
        "answer": answer,
        "answer_type": "number",
        "hint": "The hour hand moves 0.5° per minute, not just staying at the hour mark!",
        "explanation": f"At {hour}:{minute:02d}: Minute hand is at {minute}×6 = {minute_angle:.0f}°. Hour hand is at {hour}×30 + {minute}×0.5 = {hour_angle:.1f}°. Difference = {angle:.1f}°.",
        "difficulty": 2,
        "likes": random.randint(80, 220),
    }


def _profit_puzzle() -> dict:
    """Generate a profit/loss percentage puzzle."""
    total = random.choice([50, 100, 150, 200])
    profit_pct = random.randint(15, 40)
    loss_pct = random.randint(5, 15)
    profit_count = random.randint(20, min(80, total - 10))

    item = _pick(["mangoes", "apples", "oranges", "books", "pens", "toys", "candles", "bags"])

    # Overall percentage
    cost = total * 100  # assume 100 per unit cost
    revenue = profit_count * (100 + profit_pct) + (total - profit_count) * (100 - loss_pct)
    overall_pct = revenue - total * 100

    if overall_pct >= 0:
        answer = f"{overall_pct:.1f}"
        sign = "profit"
    else:
        answer = f"{abs(overall_pct):.1f}"
        sign = "loss"

    return {
        "title": f"Business Math — The {item.title()} Problem",
        "question": f"A merchant bought {total} {item}. He sold {profit_count} of them at a {profit_pct}% profit and the rest at a {loss_pct}% loss. What was his overall percentage {sign}?",
        "answer": answer,
        "answer_type": "number",
        "hint": f"Assume cost per item = 100. Calculate total cost and total revenue.",
        "explanation": f"Let cost per item = 100. Total cost = {total}×100 = {total*100}. Revenue = ({profit_count}×{100+profit_pct}) + ({total-profit_count}×{100-loss_pct}) = {profit_count*(100+profit_pct)} + {(total-profit_count)*(100-loss_pct)} = {revenue}. Overall = {overall_pct:+d} = {sign} of {abs(overall_pct):.1f}%.",
        "difficulty": 2 if sign == "profit" else 3,
        "likes": random.randint(60, 180),
    }


# ─── Sudoku Generator ───────────────────────────────────────────────────

def _make_sudoku(difficulty: int) -> tuple[str, str]:
    """
    Generate a Sudoku puzzle and its solution.

    Returns:
        (puzzle_str, solution_str)  — each is an 81-char string of digits.
        0 in puzzle_str = empty cell.
    """
    # Start from a known valid solution grid and permute
    base = [
        1, 2, 3, 4, 5, 6, 7, 8, 9,
        4, 5, 6, 7, 8, 9, 1, 2, 3,
        7, 8, 9, 1, 2, 3, 4, 5, 6,
        2, 3, 4, 5, 6, 7, 8, 9, 1,
        5, 6, 7, 8, 9, 1, 2, 3, 4,
        8, 9, 1, 2, 3, 4, 5, 6, 7,
        3, 4, 5, 6, 7, 8, 9, 1, 2,
        6, 7, 8, 9, 1, 2, 3, 4, 5,
        9, 1, 2, 3, 4, 5, 6, 7, 8,
    ]

    # Permute: relabel digits
    perm = list(range(1, 10))
    random.shuffle(perm)
    grid = [perm[val - 1] for val in base]

    # Permute: swap rows within blocks (3 groups of 3 rows)
    for block in range(3):
        rows = list(range(block * 3, block * 3 + 3))
        random.shuffle(rows)
        new_grid = grid[:]
        for i, r in enumerate(rows):
            for c in range(9):
                new_grid[block * 3 * 9 + i * 9 + c] = grid[r * 9 + c]
        grid = new_grid

    # Permute: swap columns within blocks
    for block in range(3):
        cols = list(range(block * 3, block * 3 + 3))
        random.shuffle(cols)
        new_grid = grid[:]
        for r in range(9):
            for i, c in enumerate(cols):
                new_grid[r * 9 + block * 3 + i] = grid[r * 9 + c]
        grid = new_grid

    # Transpose randomly
    if random.random() < 0.5:
        new_grid = [0] * 81
        for r in range(9):
            for c in range(9):
                new_grid[c * 9 + r] = grid[r * 9 + c]
        grid = new_grid

    solution_str = "".join(str(d) for d in grid)

    # Remove cells based on difficulty
    if difficulty == 1:  # Easy
        remove = random.randint(30, 36)
    elif difficulty == 2:  # Medium
        remove = random.randint(40, 46)
    else:  # Hard
        remove = random.randint(50, 56)

    puzzle = grid[:]
    indices = list(range(81))
    random.shuffle(indices)
    for idx in indices[:remove]:
        puzzle[idx] = 0

    puzzle_str = "".join(str(d) for d in puzzle)

    # Validate: at least 17 clues needed for unique solution
    clues = puzzle_str.count("0")
    if 81 - clues < 22:
        # Too few clues, add back some
        zero_indices = [i for i, v in enumerate(puzzle) if v == 0]
        random.shuffle(zero_indices)
        to_add = 22 - (81 - clues)
        for i in zero_indices[:to_add]:
            puzzle[i] = grid[i]
        puzzle_str = "".join(str(d) for d in puzzle)

    return puzzle_str, solution_str


def _generate_sudoku_puzzles(count: int) -> list[dict]:
    """Generate `count` Sudoku puzzles with varying difficulty."""
    puzzles = []
    for i in range(count):
        diff = random.choices([1, 2, 3], weights=[40, 35, 25])[0]
        puzzle_str, solution_str = _make_sudoku(diff)
        diff_labels = {1: "Easy", 2: "Medium", 3: "Hard"}
        puzzles.append({
            "puzzle_type": "sudoku",
            "category_id": CATEGORY_IDS["Sudoku"],
            "title": f"Sudoku #{i+1} — {diff_labels[diff]}",
            "question": puzzle_str,
            "answer": solution_str,
            "answer_type": "text",
            "hint": f"Try using the cross-hatching technique to narrow down possibilities.",
            "explanation": f"Well solved! This {diff_labels[diff].lower()} Sudoku had {81 - puzzle_str.count('0')} starting clues.",
            "difficulty": diff,
            "likes": random.randint(50, 180),
        })
    return puzzles


# ─── Math Puzzle Generator ──────────────────────────────────────────────

def _generate_math_puzzles(count: int) -> list[dict]:
    """Generate `count` math puzzles across types."""
    generators = [_sequence_puzzle, _number_trick, _age_problem, _clock_angle, _profit_puzzle]
    puzzles = []
    for i in range(count):
        gen = _pick(generators)
        puzzle = gen()
        puzzle["puzzle_type"] = "math"
        puzzle["category_id"] = CATEGORY_IDS["Math Puzzles"]
        puzzle["title"] = f"Math Puzzle #{i+1} — {puzzle['title'].split('—')[-1].strip()}"
        puzzles.append(puzzle)
    return puzzles


# ─── Logic Puzzle Pool (curated + templated) ────────────────────────────

_LOGIC_PUZZLE_TEMPLATES = [
    {
        "title": "The Two Guards",
        "question": (
            "Two doors. One leads to treasure, the other to certain doom. "
            "Two guards stand before the doors. One always tells the truth, the other always lies. "
            "You may ask ONE question to ONE guard. What question guarantees you find the treasure?"
        ),
        "answer": "What would the other guard say is the treasure door?",
        "answer_type": "text",
        "hint": "Both guards will point to the same door—the WRONG one.",
        "explanation": (
            "Ask either guard: 'What would the other guard say is the treasure door?' "
            "The liar incorrectly reports what the truth-teller would say. "
            "The truth-teller correctly reports the liar's false answer. "
            "Both point to the wrong door—choose the other!"
        ),
        "difficulty": 3,
        "likes": 320,
    },
    {
        "title": "The River Crossing",
        "question": (
            "A farmer needs to cross a river with a wolf, a goat, and a cabbage. "
            "His boat can carry him and one item at a time. "
            "If left alone: wolf eats goat, goat eats cabbage. "
            "What's the minimum number of crossings needed?"
        ),
        "answer": "7",
        "answer_type": "number",
        "hint": "The goat must come back on the 4th crossing.",
        "explanation": (
            "1. Take goat across. 2. Return alone. 3. Take wolf across. "
            "4. Bring goat BACK. 5. Take cabbage across. 6. Return alone. 7. Take goat across. Total: 7 crossings."
        ),
        "difficulty": 3,
        "likes": 280,
    },
    {
        "title": "The Three Light Switches",
        "question": (
            "You're in a room with three light switches. Each controls one of three bulbs in another room. "
            "You can flip the switches however you like, but you may enter the bulb room ONLY ONCE. "
            "How do you determine which switch controls which bulb?"
        ),
        "answer": "Turn on switch 1 for 5 mins, turn it off, turn on switch 2. Enter. On = 2. Warm off = 1. Cold off = 3.",
        "answer_type": "text",
        "hint": "What property of a light bulb changes after being on for a while?",
        "explanation": (
            "Turn switch 1 ON, wait 5 min, turn it OFF. Turn switch 2 ON. Leave switch 3 OFF. "
            "Enter bulb room: ON bulb = switch 2. OFF but WARM = switch 1. OFF and COLD = switch 3."
        ),
        "difficulty": 3,
        "likes": 450,
    },
    {
        "title": "The Poisoned Wine",
        "question": (
            "A king has 1,000 bottles of wine. Exactly one is poisoned. "
            "The poison takes 24 hours to kill. He has 10 prisoners. "
            "How can he identify the poisoned bottle in exactly 24 hours?"
        ),
        "answer": "Binary number each bottle. Each prisoner drinks from bottles with that bit=1. Wait 24h. Dead prisoners form binary number of poisoned bottle.",
        "answer_type": "text",
        "hint": "Think in binary. 2^10 = 1024 > 1000. Each prisoner represents one bit.",
        "explanation": (
            "Number bottles 1-1000 in binary (10 bits). Prisoner 1 drinks from all bottles with bit 1 = 1, "
            "prisoner 2 from all with bit 2 = 1, etc. After 24h, the pattern of dead prisoners gives the "
            "binary number of the poisoned bottle."
        ),
        "difficulty": 4,
        "likes": 380,
    },
    {
        "title": "The 12 Coin Problem",
        "question": (
            "You have 12 identical-looking coins. One is counterfeit—it's either heavier or lighter. "
            "You have a balance scale. What's the minimum number of weighings needed to find the fake?"
        ),
        "answer": "3",
        "answer_type": "number",
        "hint": "Divide into 3 groups of 4, not 2 groups of 6!",
        "explanation": (
            "Weigh 4 vs 4. If balanced, fake is in the remaining 4 (1 more weighing identifies it). "
            "If not balanced, the fake is among the 8. The key insight is each weighing gives "
            "3 possible outcomes (left heavy, right heavy, balanced), so 3 weighings = 3^3 = 27 possibilities > 24 possibilities (12 coins × 2)."
        ),
        "difficulty": 4,
        "likes": 310,
    },
    {
        "title": "The Knights and Knaves",
        "question": (
            "You meet two people on an island. Knights always tell the truth, knaves always lie. "
            "Person A says: 'B is a knight.' Person B says: 'We are different types.' "
            "What are A and B?"
        ),
        "answer": "A is a knave, B is a knight",
        "answer_type": "text",
        "hint": "If A were a knight telling the truth, what would B be? Would B's statement then be true?",
        "explanation": (
            "If A is a knight (truth-teller), B is a knight. But then B says 'we are different types'—which would be a lie. "
            "Contradiction! So A must be a knave. Then B is a knave too (A lies). B says 'we are different'—"
            "they're the same type, so B lies, which is consistent. So A = knave, B = knight."
        ),
        "difficulty": 3,
        "likes": 260,
    },
    {
        "title": "The Bridge Crossing",
        "question": (
            "Four people need to cross a bridge at night. They have one torch. "
            "Crossing times: A=1 min, B=2 min, C=5 min, D=10 min. "
            "The bridge holds max 2 people at a time. When two cross, they go at the slower pace. "
            "What's the minimum total time for all four to cross?"
        ),
        "answer": "17",
        "answer_type": "number",
        "hint": "The two slowest (5 and 10) should cross together. But someone needs to bring the torch back.",
        "explanation": (
            "1. A+B cross (2 min). 2. A returns (1 min). 3. C+D cross (10 min). 4. B returns (2 min). "
            "5. A+B cross (2 min). Total = 2+1+10+2+2 = 17 minutes."
        ),
        "difficulty": 3,
        "likes": 340,
    },
    {
        "title": "The Birthday Paradox",
        "question": (
            "In a room of 23 people, what's the approximate probability (as a percentage) "
            "that at least two people share the same birthday? "
            "(Assume 365 days, no leap years.)"
        ),
        "answer": "50",
        "answer_type": "number",
        "hint": "It's easier to calculate the probability that NO ONE shares a birthday, then subtract from 1.",
        "explanation": (
            "P(no match) = 365/365 × 364/365 × 363/365 × ... × 343/365 ≈ 0.493. "
            "So P(at least one match) ≈ 1 - 0.493 = 0.507 ≈ 50.7%. The answer is about 50%."
        ),
        "difficulty": 3,
        "likes": 290,
    },
    {
        "title": "The Monty Hall Problem",
        "question": (
            "You're on a game show. Three doors: one car, two goats. "
            "You pick door 1. The host (who knows what's behind each door) opens door 3, revealing a goat. "
            "He asks: 'Do you want to switch to door 2?' "
            "Should you switch to maximize your chance of winning the car?",
        ),
        "answer": "Yes, switching gives 2/3 chance",
        "answer_type": "text",
        "hint": "The host always reveals a goat. What was your chance of picking wrong initially?",
        "explanation": (
            "Initially: chance of picking car = 1/3, goat = 2/3. "
            "If you picked goat (2/3 chance), the host reveals the other goat, and switching WINS. "
            "If you picked car (1/3), switching LOSES. So switching wins 2/3 of the time—always switch!"
        ),
        "difficulty": 2,
        "likes": 520,
    },
    {
        "title": "The Missing Dollar",
        "question": (
            "Three friends pay $30 for a hotel room ($10 each). "
            "The clerk realizes the room is only $25 and gives $5 to the bellboy to return. "
            "The bellboy keeps $2 and gives $1 back to each friend. "
            "Now each friend paid $9 (3 × $9 = $27), plus the $2 the bellboy kept = $29. "
            "Where did the missing dollar go?"
        ),
        "answer": "There is no missing dollar",
        "answer_type": "text",
        "hint": "It's a trick! The $27 already includes the $2 kept by the bellboy.",
        "explanation": (
            "There is no missing dollar. The $27 paid includes the $25 room + $2 kept by the bellboy. "
            "The $3 returned is separate: $27 + $3 = $30. The riddle's '27+2' is a misdirection—"
            "it double-counts the $2."
        ),
        "difficulty": 2,
        "likes": 410,
    },
]


def _generate_logic_puzzles(count: int) -> list[dict]:
    """Generate `count` logic puzzles — curated pool + templated."""
    puzzles = []
    # Use curated templates (cycle through them)
    for i in range(count):
        template = _LOGIC_PUZZLE_TEMPLATES[i % len(_LOGIC_PUZZLE_TEMPLATES)]
        puzzle = dict(template)
        puzzle["puzzle_type"] = "logic"
        puzzle["category_id"] = CATEGORY_IDS["Logic Puzzles"]
        puzzle["title"] = f"Logic Puzzle #{i+1} — {template['title']}"
        puzzle["likes"] = template["likes"] + random.randint(-20, 20)
        puzzles.append(puzzle)
    return puzzles


# ─── Word Puzzle Pool (curated + generated) ─────────────────────────────

_WORD_RIDDLES = [
    {
        "title": "I Speak Without a Mouth",
        "question": "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
        "answer": "echo",
        "hint": "Think about what happens when you shout in a valley.",
        "explanation": "An echo is a reflection of sound that 'speaks back.' It needs air to carry sound waves.",
    },
    {
        "title": "The More You Take, the More You Leave Behind",
        "question": "The more you take, the more you leave behind. What am I?",
        "answer": "footsteps",
        "hint": "Think about walking on a sandy beach.",
        "explanation": "Footsteps! Each step you take leaves a footprint behind. The more steps you take, the more footprints you leave.",
    },
    {
        "title": "I Have Cities but No Houses",
        "question": "I have cities, but no houses. I have mountains, but no trees. I have rivers, but no water. What am I?",
        "answer": "map",
        "hint": "You might use this when traveling or studying geography.",
        "explanation": "A map represents geographical features but doesn't contain the actual objects.",
    },
    {
        "title": "What Gets Wetter the More It Dries?",
        "question": "What gets wetter the more it dries?",
        "answer": "towel",
        "hint": "Think about what happens after a bath or shower.",
        "explanation": "A towel! It dries you off but gets wetter itself in the process.",
    },
    {
        "title": "I Can Be Cracked, Made, Told, and Played",
        "question": "I can be cracked, made, told, and played. What am I?",
        "answer": "joke",
        "hint": "Comedians are experts with this.",
        "explanation": "A joke! You crack a joke, make a joke, tell a joke, and play a joke.",
    },
    {
        "title": "What Has a Head and a Tail but No Body?",
        "question": "What has a head and a tail but no body?",
        "answer": "coin",
        "hint": "Think about something you might flip.",
        "explanation": "A coin! It has a head side and a tail side, but no body.",
    },
    {
        "title": "The Man Who Invented It Doesn't Want It",
        "question": "The man who invented it doesn't want it. The man who bought it doesn't need it. The man who needs it doesn't know it. What is it?",
        "answer": "coffin",
        "hint": "This is about the final necessity of life.",
        "explanation": "A coffin! The inventor doesn't want to use it, the buyer (a friend/family) doesn't need it, and the person who needs it is deceased.",
    },
    {
        "title": "What Comes Once in a Minute, Twice in a Moment, but Never in a Thousand Years?",
        "question": "What comes once in a minute, twice in a moment, but never in a thousand years?",
        "answer": "m",
        "hint": "Look at the letters in each word.",
        "explanation": "The letter 'M'! It appears once in 'minute', twice in 'moment', and not at all in 'thousand years'.",
    },
    {
        "title": "I Follow You All Day Long",
        "question": "I follow you all day long, but when the sun goes down I disappear. What am I?",
        "answer": "shadow",
        "hint": "You can see me on a sunny day but not at midnight.",
        "explanation": "A shadow! It follows you everywhere during daylight but vanishes when the sun sets.",
    },
    {
        "title": "What Can Travel Around the World While Staying in a Corner?",
        "question": "What can travel around the world while staying in a corner?",
        "answer": "stamp",
        "hint": "You put this on an envelope.",
        "explanation": "A postage stamp! It can travel the world on a letter while sitting in the corner of an envelope.",
    },
    {
        "title": "I Have Keys but No Locks",
        "question": "I have keys but no locks. I have space but no room. You can enter but can't go inside. What am I?",
        "answer": "keyboard",
        "hint": "You're probably using one right now!",
        "explanation": "A keyboard! It has keys (buttons), space (space bar), and you can 'enter' (Enter key), but you can't go inside it.",
    },
    {
        "title": "What Can You Break Even If You Never Pick It Up or Touch It?",
        "question": "What can you break even if you never pick it up or touch it?",
        "answer": "promise",
        "hint": "It's something you make with words.",
        "explanation": "A promise! You can break a promise without physically touching it.",
    },
    {
        "title": "I Am Taken from a Mine and Shut Up in a Wooden Case",
        "question": "I am taken from a mine and shut up in a wooden case, from which I am never released. What am I?",
        "answer": "pencil lead",
        "hint": "You might use this to write.",
        "explanation": "Pencil lead (graphite)! It's mined from the ground and encased in wood. It's never 'released' from the pencil—it wears away as you write.",
    },
    {
        "title": "What Is Always Coming but Never Arrives?",
        "question": "What is always coming but never arrives?",
        "answer": "tomorrow",
        "hint": "Think about the calendar.",
        "explanation": "Tomorrow! It's always the next day but the moment it arrives, it becomes 'today.'",
    },
    {
        "title": "I Can Be Long or Short",
        "question": "I can be long or short, I can be grown or made, I can be painted or left bare, I can be round or square. What am I?",
        "answer": "fingernails",
        "hint": "Everyone has these on their hands.",
        "explanation": "Fingernails! They grow long or can be cut short, can be painted or natural.",
    },
    {
        "title": "What Runs All Around a Backyard Yet Never Moves?",
        "question": "What runs all around a backyard yet never moves?",
        "answer": "fence",
        "hint": "It marks the boundary of the yard.",
        "explanation": "A fence! It 'runs' around the perimeter but stays in place.",
    },
    {
        "title": "What Has Many Teeth but Can't Bite?",
        "question": "What has many teeth but can't bite?",
        "answer": "comb",
        "hint": "You use this on your head.",
        "explanation": "A comb! It has many 'teeth' but they're for grooming hair, not biting.",
    },
    {
        "title": "What Can Fill a Room but Takes Up No Space?",
        "question": "What can fill a room but takes up no space?",
        "answer": "light",
        "hint": "It's essential for vision.",
        "explanation": "Light! It illuminates a room but doesn't occupy physical volume.",
    },
    {
        "title": "I Am Not Alive but I Grow; I Don't Have Lungs but I Need Air",
        "question": "I am not alive but I grow; I don't have lungs but I need air. What am I?",
        "answer": "fire",
        "hint": "Campers and firefighters know this well.",
        "explanation": "Fire! It grows when fueled, needs oxygen to burn, but isn't a living thing.",
    },
    {
        "title": "What Starts with E, Ends with E, but Only Has One Letter?",
        "question": "What starts with E, ends with E, but only has one letter?",
        "answer": "envelope",
        "hint": "Think about mail.",
        "explanation": "An envelope! It starts with 'e', ends with 'e', and contains a letter (the message inside).",
    },
]


_ANAGRAM_POOL = [
    ("listen", "silent", "What you should be doing right now"),
    ("dormitory", "dirty room", "This is a phrase, not a single word"),
    ("the eyes", "they see", "Think about vision"),
    ("a gentleman", "elegant man", "A polite fellow"),
    ("twelve plus one", "eleven plus two", "A math phrase"),
    ("schoolmaster", "the classroom", "Where learning happens"),
    ("astronomer", "moon starer", "Someone who studies the stars"),
    ("debit card", "bad credit", "A financial phrase"),
    ("funeral", "real fun", "A surprising mix of emotions"),
    ("the Morse code", "here comes dot", "Communication system"),
    ("the earthquakes", "that queer shake", "Natural disaster phenomenon"),
    ("slot machines", "cash lost in 'em", "Gambling wisdom"),
    ("conversation", "voice resonates", "A meaningful chat"),
    ("desperation", "a rope ends it", "Poetic but dark"),
    ("semolina", "is no meal", "A kitchen puzzle"),
    ("a decimal point", "I am a dot in place", "Math concept"),
    ("punishment", "nine thumps", "Old-school discipline"),
    ("the nudist colony", "no untidy clothes", "Beach lifestyle"),
    ("circumstantial", "claims: it's a run", "Indirect evidence"),
    ("adultery", "real duty", "What a wedding is"),
    ("the detectives", "detect thieves", "Who solves crimes"),
]


_PALINDROME_POOL = [
    "racecar",
    "level",
    "radar",
    "madam",
    "refer",
    "civic",
    "kayak",
    "tenet",
    "rotor",
    "noon",
    "stats",
    "deified",
    "repoper",
    "redder",
    "solos",
]


def _generate_word_puzzles(count: int) -> list[dict]:
    """Generate `count` word puzzles — riddles, anagrams, palindromes."""
    puzzles = []
    types = ["riddle", "anagram", "palindrome"]
    for i in range(count):
        ptype = types[i % len(types)]

        if ptype == "riddle":
            riddle = _WORD_RIDDLES[i % len(_WORD_RIDDLES)]
            puzzle = {
                "puzzle_type": "word",
                "category_id": CATEGORY_IDS["Word Puzzles"],
                "title": f"Word Riddle #{i+1} — {riddle['title']}",
                "question": riddle["question"],
                "answer": riddle["answer"],
                "answer_type": "text",
                "hint": riddle["hint"],
                "explanation": riddle["explanation"],
                "difficulty": 1,
                "likes": random.randint(80, 250),
            }

        elif ptype == "anagram":
            scrambled, solution, hint = _ANAGRAM_POOL[i % len(_ANAGRAM_POOL)]
            puzzle = {
                "puzzle_type": "word",
                "category_id": CATEGORY_IDS["Word Puzzles"],
                "title": f"Anagram #{i+1} — Unscramble It!",
                "question": f"Rearrange the letters of '{scrambled.upper()}' to form a new word or phrase. What is it?",
                "answer": solution,
                "answer_type": "text",
                "hint": hint,
                "explanation": f"'{scrambled.title()}' rearranged forms '{solution.title()}'. The letters are exactly the same!",
                "difficulty": 2,
                "likes": random.randint(60, 200),
            }

        else:  # palindrome
            word = _PALINDROME_POOL[i % len(_PALINDROME_POOL)]
            hint_word = word[:len(word)//2]
            puzzle = {
                "puzzle_type": "word",
                "category_id": CATEGORY_IDS["Word Puzzles"],
                "title": f"Palindrome #{i+1} — Forward and Backward",
                "question": f"I'm a word that reads the same forward and backward. I start with '{word[0]}' and have {len(word)} letters. What word am I?\n\nHint: {'_ ' * len(word)} ({len(word)} letters)",
                "answer": word,
                "answer_type": "text",
                "hint": f"My first {len(word)//2} letters are '{hint_word}'.",
                "explanation": f"'{word}' is a palindrome — it reads the same forward and backward!",
                "difficulty": 1,
                "likes": random.randint(30, 120),
            }

        puzzles.append(puzzle)
    return puzzles


# ─── Puzzle Type Router ─────────────────────────────────────────────────

# Distribution weights by type
TYPE_DISTRIBUTION = {
    "sudoku": 0.20,
    "math": 0.30,
    "logic": 0.25,
    "word": 0.25,
}


def generate_puzzles(total: int) -> list[dict]:
    """Generate `total` puzzles distributed across all 4 types."""
    counts = {}
    remaining = total
    types = list(TYPE_DISTRIBUTION.keys())
    weights = list(TYPE_DISTRIBUTION.values())

    for t, w in zip(types, weights):
        counts[t] = max(1, int(total * w))
        remaining -= counts[t]

    # Distribute remaining
    for i in range(remaining):
        counts[types[i % len(types)]] += 1

    all_puzzles = []
    all_puzzles.extend(_generate_sudoku_puzzles(counts["sudoku"]))
    all_puzzles.extend(_generate_math_puzzles(counts["math"]))
    all_puzzles.extend(_generate_logic_puzzles(counts["logic"]))
    all_puzzles.extend(_generate_word_puzzles(counts["word"]))

    random.shuffle(all_puzzles)
    return all_puzzles[:total]
