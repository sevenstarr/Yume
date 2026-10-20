"""Yume S6 migration/rollback regression on a synthetic SQLite copy; assisted by Codex."""

import hashlib
import json
import sqlite3
from pathlib import Path


def verify():
    root = Path(__file__).resolve().parents[1]
    fixture = root / "data/src/main/sqldelight/148.db"
    migration = root / "data/src/main/sqldelight/tachiyomi/migrations/148.sqm"
    rollback = root / "scripts/sql/rollback-metadata-148.sql"
    db = sqlite3.connect(":memory:")
    with sqlite3.connect(f"file:{fixture.as_posix()}?mode=ro", uri=True) as baseline:
        baseline.backup(db)
    db.execute("PRAGMA foreign_keys=ON")
    db.executescript("""
        INSERT INTO animes(_id,source,url,title,status,initialized,viewer,episode_flags,
            cover_last_modified,season_flags,season_number,season_source_order,background_last_modified,
            favorite_at,thumbnail_url,custom_title,custom_artist,custom_author,custom_description,
            custom_genre,custom_status)
        VALUES(1,42,'/anime','Original title',2,1,3,4,123,5,-1,0,456,1000,
            'https://source.invalid/original.jpg','Custom title','Artist','Author','Description','Drama',2);
        INSERT INTO episodes(_id,anime_id,url,name,seen,bookmark,fillermark,last_second_seen,
            total_seconds,episode_number,source_order,date_fetch,date_upload)
        VALUES(1,1,'/episode','Episode',1,1,1,777,1400,1,1,1000,900);
        INSERT INTO history(_id,episode_id,last_seen) VALUES(1,1,2000);
        INSERT INTO categories(_id,name,sort,flags) VALUES(1,'Favorites',1,0);
        INSERT INTO animes_categories(anime_id,category_id) VALUES(1,1);
        INSERT INTO anime_sync(_id,anime_id,sync_id,remote_id,title,last_episode_seen,
            total_episodes,status,score,remote_url,start_date,finish_date)
        VALUES(1,1,2,1,'Tracked title',1,26,1,9,'https://anilist.co/anime/1',0,0);
    """)
    tables = [row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
    original_schema = list(db.execute("SELECT name,sql FROM sqlite_master ORDER BY name"))

    def rows():
        return {name: list(db.execute(f'SELECT * FROM "{name}" ORDER BY rowid')) for name in tables}

    original_rows = rows()
    db.executescript("BEGIN;\n" + migration.read_text(encoding="utf-8") + "\nCOMMIT;")
    assert rows() == original_rows, "Migration changed legacy rows"
    for name, sql in original_schema:
        assert db.execute("SELECT sql FROM sqlite_master WHERE name=?", (name,)).fetchone() == (sql,)
    assert not list(db.execute("PRAGMA foreign_key_check"))
    db.execute("INSERT INTO metadata_control(singleton) VALUES(1)")
    assert db.execute("SELECT enabled,epoch,host_not_before FROM metadata_control").fetchone() == (0, 0, 0)
    db.execute("INSERT INTO anime_metadata(anime_id,payload) VALUES(1,'{}')")
    db.execute("INSERT INTO metadata_work(anime_id,epoch,lease_until,lease_id) VALUES(1,0,100,'synthetic-lease')")
    db.commit()
    db.executescript(rollback.read_text(encoding="utf-8"))
    assert rows() == original_rows, "Rollback changed legacy rows"
    assert list(db.execute("SELECT name,sql FROM sqlite_master ORDER BY name")) == original_schema
    db.close()
    return {
        "fixture": "synthetic schema 148, no user data",
        "fixtureSha256": hashlib.sha256(fixture.read_bytes()).hexdigest(),
        "migrationSha256": hashlib.sha256(migration.read_bytes()).hexdigest(),
        "legacyTablesCompared": len(tables),
        "seededTables": [name for name, entries in original_rows.items() if entries],
        "checks": ["upgrade preserves legacy rows/schema", "foreign keys", "default off", "rollback preserves legacy rows/schema"],
        "result": "passed",
    }


if __name__ == "__main__":
    print(json.dumps(verify(), indent=2))
