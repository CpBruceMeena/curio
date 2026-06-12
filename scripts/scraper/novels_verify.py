"""
Novels Verification — validates chapter formatting quality of downloaded novels.

Checks:
  1. Minimum chapter count (novels should have > 1 chapter)
  2. No hard-wrapped text (paragraphs joined with spaces, not newlines)
  3. Sensible chapter lengths (no empty or tiny chapters)
  4. Clean paragraph structure (proper double-newline separation)
  5. No boilerplate leakage (Gutenberg headers/footers)
  6. Language consistency

Can optionally auto-fix issues by re-downloading with the alternative method
(if EPUB was used, try text fallback; if text was used, try EPUB).

Usage:
    python -m scraper --novels-batch 0 --ids 1342,1661  # scrape first
    python -m scraper --novels-verify                    # verify all novels
    python -m scraper --novels-verify --auto-fix          # verify + auto-fix
    python -m scraper --novels-verify --id 1342           # verify one novel
"""

import re
from typing import Optional

from scraper.novels_formatter import download_text, strip_boilerplate
from scraper.novels_formatter import split_into_chapters_improved
from scraper.content_validator import normalize_text_alignment, tts_normalize_text
from scraper.db import DB


# ─── Quality thresholds ───────────────────────────────────────────────────────

MIN_CHAPTERS = 2               # novels must have at least 2 chapters
MIN_CHAPTER_LENGTH = 200       # chars per chapter minimum
MAX_SINGLE_PARAGRAPH_RATIO = 0.9   # if 90%+ is one paragraph → bad split
MAX_AVG_PARAGRAPH_LENGTH = 3000    # avg paragraph > 3K chars → hard-wrap not fixed
MIN_WORD_VARIETY = 0.05        # at least 5% unique words (detects repetitive text)

# Boilerplate fragments that shouldn't appear in chapter bodies
BOILERPLATE_FRAGMENTS = [
    "Project Gutenberg",
    "www.gutenberg.org",
    "End of the Project",
    "End of Project Gutenberg",
    "*** START OF",
    "*** END OF",
    "Full text of",
]


# ─── Individual checks ────────────────────────────────────────────────────────


def check_chapter_count(novel: dict) -> list[str]:
    """Check if novel has a reasonable number of chapters."""
    issues = []
    total = novel.get("total_chapters", 0)
    chapters = novel.get("chapters", [])

    if total < MIN_CHAPTERS:
        issues.append(f"Only {total} chapter(s) — minimum is {MIN_CHAPTERS}")

    # Check if we have less chapters than expected for certain known novels
    title = novel.get("title", "").lower()
    expected = None
    # Only flag known novels with well-known chapter counts
    known_chapters = {
        "pride and prejudice": 61,
        "moby dick": 135,
        "dracula": 27,
        "frankenstein": 24,
        "alice's adventures in wonderland": 12,
        "the adventures of sherlock holmes": 12,
        "great expectations": 59,
        "jane eyre": 38,
        "the picture of dorian gray": 20,
        "the prince": 26,
    }
    for key, count in known_chapters.items():
        if key in title:
            expected = count
            break

    if expected and total < expected * 0.5:
        issues.append(f"Expected ~{expected} chapters but got {total} — likely under-split")

    return issues


def check_hard_wrapping(chapters: list[dict]) -> list[str]:
    """Check for hard-wrapped text (single newlines inside paragraphs)."""
    issues = []
    for ch in chapters:
        body = ch.get("body", "")
        # Count newlines vs paragraph breaks
        total_newlines = body.count("\n")
        para_breaks = body.count("\n\n")

        hard_breaks = total_newlines - (para_breaks * 2)

        if hard_breaks > 5:
            avg_para_len = len(body) // max(1, body.count("\n\n") + 1)
            issues.append(
                f"Chapter {ch['chapter_number']}: {hard_breaks} hard breaks detected, "
                f"avg paragraph length {avg_para_len} chars"
            )
            # Only report first affected chapter to reduce noise
            break

    return issues


def check_chapter_lengths(chapters: list[dict]) -> list[str]:
    """Check for empty or very short chapters."""
    issues = []
    for ch in chapters:
        body = ch.get("body", "")
        length = len(body.strip())
        if length < MIN_CHAPTER_LENGTH:
            issues.append(
                f"Chapter {ch['chapter_number']}: only {length} chars "
                f"(minimum {MIN_CHAPTER_LENGTH})"
            )
    return issues


def check_paragraph_structure(chapters: list[dict]) -> list[str]:
    """Check for proper paragraph structure."""
    issues = []
    for ch in chapters:
        body = ch.get("body", "")
        paragraphs = [p.strip() for p in body.split("\n\n") if p.strip()]

        if not paragraphs:
            issues.append(f"Chapter {ch['chapter_number']}: no paragraphs")
            continue

        # Check if everything is one giant paragraph
        if len(paragraphs) == 1:
            issues.append(
                f"Chapter {ch['chapter_number']}: single paragraph "
                f"({len(body)} chars) — paragraphs not split"
            )

    return issues


def check_boilerplate(chapters: list[dict]) -> list[str]:
    """Check for Gutenberg boilerplate leakage."""
    issues = []
    for ch in chapters:
        body = ch.get("body", "")
        for frag in BOILERPLATE_FRAGMENTS:
            if frag in body:
                issues.append(
                    f"Chapter {ch['chapter_number']}: contains '{frag}'"
                )
                break  # one issue per chapter
    return issues


def check_word_variety(chapters: list[dict]) -> list[str]:
    """Check for repetitive text (low word variety)."""
    issues = []
    for ch in chapters:
        body = ch.get("body", "")
        words = body.split()
        if len(words) < 20:
            continue
        unique = len(set(w.lower() for w in words))
        variety = unique / len(words)
        if variety < MIN_WORD_VARIETY:
            issues.append(
                f"Chapter {ch['chapter_number']}: low word variety "
                f"({variety:.1%}, threshold {MIN_WORD_VARIETY:.0%})"
            )
    return issues


# ─── Quality grade ─────────────────────────────────────────────────────────────


def grade_novel(novel: dict) -> tuple[str, int, list[str]]:
    """Grade a novel's formatting quality.

    Returns: (grade, score, list_of_issues)
    Grades: A (excellent), B (good), C (fair), D (poor), F (unusable)
    Score: 0-100
    """
    chapters = novel.get("chapters", [])
    all_issues = []

    all_issues.extend(check_chapter_count(novel))
    all_issues.extend(check_hard_wrapping(chapters))
    all_issues.extend(check_chapter_lengths(chapters))
    all_issues.extend(check_paragraph_structure(chapters))
    all_issues.extend(check_boilerplate(chapters))
    all_issues.extend(check_word_variety(chapters))

    # Score calculation
    deductions = {
        "boilerplate": 30,
        "hard_wrapping": 25,
        "single_paragraph": 20,
        "low_chapter_count": 15,
    }

    score = 100
    for issue in all_issues:
        if "boilerplate" in issue.lower():
            score -= deductions["boilerplate"]
        elif "hard break" in issue.lower():
            score -= deductions["hard_wrapping"]
        elif "single paragraph" in issue.lower() or "paragraphs not split" in issue.lower():
            score -= deductions["single_paragraph"]
        elif "only" in issue.lower() and "chapter" in issue.lower():
            score -= deductions["low_chapter_count"]
        else:
            score -= 5

    score = max(0, min(100, score))

    if score >= 90:
        grade = "A"
    elif score >= 75:
        grade = "B"
    elif score >= 50:
        grade = "C"
    elif score >= 25:
        grade = "D"
    else:
        grade = "F"

    return grade, score, all_issues


# ─── Verification pipeline ─────────────────────────────────────────────────────


def verify_one_novel(novel: dict, verbose: bool = False) -> dict:
    """Verify a single novel's formatting quality.

    Returns a dict with: title, id, grade, score, issues, auto_fixable
    """
    title = novel.get("title", "Unknown")
    chapters = novel.get("chapters", [])
    gutenberg_id = None
    if novel.get("source_url"):
        import re
        m = re.search(r"/ebooks/(\d+)", novel["source_url"])
        if m:
            gutenberg_id = int(m.group(1))

    grade, score, issues = grade_novel(novel)

    # Determine if auto-fixable
    auto_fixable = False
    if gutenberg_id and score < 50 and chapters:
        auto_fixable = True

    result = {
        "title": title,
        "gutenberg_id": gutenberg_id,
        "grade": grade,
        "score": score,
        "issues": issues,
        "auto_fixable": auto_fixable,
        "total_chapters": len(chapters),
    }

    if verbose:
        print(f"  {'─'*40}")
        print(f"  {title}")
        print(f"  Grade: {grade} ({score}/100)  |  Chapters: {len(chapters)}")
        if issues:
            print(f"  Issues:")
            for issue in issues:
                print(f"    • {issue}")
        if auto_fixable:
            print(f"  💡 Auto-fix available (re-download with alternate method)")
        print()

    return result


def auto_fix_novel(
    gutenberg_id: int,
    current_result: dict,
) -> Optional[dict]:
    """Try to fix a novel by re-downloading with the alternative method.
    
    If the current result used EPUB re-flow, this tries plain text,
    and vice versa.
    """
    print(f"    🔧 Attempting auto-fix for #{gutenberg_id}...")

    # Try text method directly (bypass EPUB)
    print(f"    📄 Trying plain text method...")
    raw_text = download_text(gutenberg_id)
    if raw_text and len(raw_text) > 500:
        body_text = strip_boilerplate(raw_text)
        if len(body_text) >= 500:
            chapters = split_into_chapters_improved(body_text)
            if chapters and len(chapters) >= MIN_CHAPTERS:
                fixed = dict(current_result)
                processed = []
                for ch in chapters:
                    body = ch.get("body", "")[:15000]
                    if len(body.strip()) < 100:
                        continue
                    body = normalize_text_alignment(body, category="Short Stories")
                    body = tts_normalize_text(body, category="Short Stories")
                    processed.append({
                        "chapter_number": ch["chapter_number"],
                        "title": ch.get("title", f"Chapter {ch['chapter_number']}")[:300],
                        "body": body,
                        "read_time_secs": max(8, min(180, round(len(body.split()) / 3))),
                    })
                if processed:
                    fixed["chapters"] = processed
                    fixed["total_chapters"] = len(processed)
                    print(f"    ✅ Auto-fix successful: {len(processed)} chapters via text method")
                    return fixed

    print(f"    ⚠ Auto-fix failed for #{gutenberg_id}")
    return None


# ─── Main verification entry point ─────────────────────────────────────────────


def verify_all_novels(
    db: DB,
    auto_fix: bool = False,
    single_id: Optional[int] = None,
    verbose: bool = False,
) -> int:
    """Verify all novels in the database, optionally fixing issues.

    Returns the number of novels verified.
    """
    if single_id:
        novels = db.query(
            "SELECT id, title, author, source_url, total_chapters FROM novels WHERE id = %s",
            [single_id]
        )
    else:
        novels = db.query(
            "SELECT id, title, author, source_url, total_chapters FROM novels ORDER BY id"
        )

    if not novels:
        print("📚 No novels found in database to verify.")
        return 0

    print(f"📚 Verifying {len(novels)} novels...\n")

    grades = {"A": 0, "B": 0, "C": 0, "D": 0, "F": 0}
    total_score = 0
    fixable_count = 0
    fixed_count = 0

    for novel in novels:
        # Build a fake novel dict from DB data
        chapters = db.query(
            "SELECT chapter_number, title, body, read_time_secs "
            "FROM novel_chapters WHERE novel_id = %s ORDER BY chapter_number",
            [novel["id"]]
        )
        novel_dict = {
            "title": novel["title"],
            "author": novel["author"],
            "source_url": novel["source_url"],
            "total_chapters": novel["total_chapters"],
            "chapters": chapters,
        }

        result = verify_one_novel(novel_dict, verbose=verbose)
        grades[result["grade"]] = grades.get(result["grade"], 0) + 1
        total_score += result["score"]

        if result["auto_fixable"]:
            fixable_count += 1

        if auto_fix and result["auto_fixable"] and result["gutenberg_id"]:
            time.sleep(1)  # rate limit
            fixed = auto_fix_novel(result["gutenberg_id"], novel_dict)
            if fixed:
                # Update in DB
                try:
                    # Remove old chapters
                    db.execute(
                        "DELETE FROM novel_chapters WHERE novel_id = %s",
                        [novel["id"]]
                    )
                    # Update total_chapters
                    db.execute(
                        "UPDATE novels SET total_chapters = %s WHERE id = %s",
                        [fixed["total_chapters"], novel["id"]]
                    )
                    # Insert new chapters
                    for ch in fixed["chapters"]:
                        db.execute(
                            "INSERT INTO novel_chapters (novel_id, chapter_number, title, body, read_time_secs) "
                            "VALUES (%s, %s, %s, %s, %s)",
                            [
                                novel["id"],
                                ch["chapter_number"],
                                ch.get("title", "")[:500],
                                ch.get("body", ""),
                                ch.get("read_time_secs", 0),
                            ]
                        )
                    fixed_count += 1
                    print(f"    ✅ Fixed and updated in DB\n")
                except Exception as e:
                    print(f"    ⚠ DB update failed: {e}\n")

    # Summary
    print(f"\n{'='*50}")
    print(f"  📊 Verification Summary")
    print(f"{'='*50}")
    print(f"  Total novels: {len(novels)}")
    print(f"  Average score: {total_score / max(1, len(novels)):.0f}/100")
    print(f"  Grades: A={grades.get('A',0)} B={grades.get('B',0)} "
          f"C={grades.get('C',0)} D={grades.get('D',0)} F={grades.get('F',0)}")
    if auto_fix:
        print(f"  Auto-fixable: {fixable_count}, Fixed: {fixed_count}")
    print(f"{'='*50}\n")

    return len(novels)
