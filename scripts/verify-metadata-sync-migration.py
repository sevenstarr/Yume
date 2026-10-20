"""Yume S9: synthetic SQLite migration and rollback verification; assisted by Codex."""

import hashlib
import json
import sqlite3
from pathlib import Path


def verify():
    root = Path(__file__).resolve().parents[1]
    fixture = root / "data/src/main/sqldelight/149.db"
    migration = root / "data/src/main/sqldelight/tachiyomi/migrations/149.sqm"
    db = sqlite3.connect(":memory:")
    with sqlite3.connect(f"file:{fixture.as_posix()}?mode=ro", uri=True) as baseline:
        baseline.backup(db)
    db.execute("PRAGMA foreign_keys=ON")
    db.executescript("""
        INSERT INTO animes(_id,source,url,title,status,initialized,viewer,episode_flags,
        cover_last_modified,season_flags,season_number,season_source_order,background_last_modified,
        favorite_at,custom_title)
        VALUES(1,42,'/test','Original title',2,1,3,4,123,5,-1,0,456,1000,'Custom title');
        INSERT INTO metadata_control(singleton,enabled,epoch,host_not_before) VALUES(1,1,7,123456);
        INSERT INTO anime_metadata(anime_id,payload,revision,generation) VALUES(1,'synthetic decision',4,6);
    """)
    tables = [row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
    schema = list(db.execute("SELECT name,sql FROM sqlite_master ORDER BY name"))

    def rows():
        return {name: list(db.execute(f'SELECT * FROM "{name}" ORDER BY rowid')) for name in tables}

    original = rows()
    db.executescript("BEGIN;\n" + migration.read_text(encoding="utf-8") + "\nCOMMIT;")
    assert rows() == original
    for name, sql in schema:
        assert db.execute("SELECT sql FROM sqlite_master WHERE name=?", (name,)).fetchone() == (sql,)
    db.execute("INSERT INTO metadata_sync_control(singleton) VALUES(1)")
    assert db.execute("SELECT generation,epoch,status,on_add,charging FROM metadata_sync_control").fetchone() == (
        0, 0, "IDLE", 0, 0
    )
    db.execute("INSERT INTO metadata_sync_queue(anime_id) VALUES(1)")
    assert not list(db.execute("PRAGMA foreign_key_check"))
    db.executescript("DROP TABLE metadata_sync_queue; DROP TABLE metadata_sync_control;")
    assert rows() == original
    assert list(db.execute("SELECT name,sql FROM sqlite_master ORDER BY name")) == schema
    db.close()
    return {
        "fixture": "synthetic schema 149, no user data",
        "fixtureSha256": hashlib.sha256(fixture.read_bytes()).hexdigest(),
        "migrationSha256": hashlib.sha256(migration.read_bytes()).hexdigest(),
        "existingTablesCompared": len(tables),
        "checks": ["upgrade preserves existing rows/schema", "default idle and opt-in off", "foreign keys", "rollback"],
        "result": "passed",
    }


if __name__ == "__main__":
    print(json.dumps(verify(), indent=2))
