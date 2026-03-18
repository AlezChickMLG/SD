CREATE TABLE IF NOT EXISTS account (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS buget (
    user_id INTEGER PRIMARY KEY,
    intretinere REAL NOT NULL,
    mancare REAL NOT NULL,
    distractie REAL NOT NULL,
    scoala REAL NOT NULL,
    personale REAL NOT NULL,
    FOREIGN KEY (user_id) REFERENCES account(id)
);

PRAGMA foreign_keys = ON;