import sqlite3
import json

class database:

  def __init__(self, dbpath):
    self.dbpath = dbpath
    

  def execute_query(self, query):
    try:
        with sqlite3.connect(self.dbpath) as conn:
            conn.row_factory = sqlite3.Row
            conn.execute("PRAGMA foreign_keys = ON") #enable foreign keys
            cur = conn.cursor()
            query_res = cur.execute(query)
            dict_array = []
            for r in query_res.fetchall():
                dict_array.append(dict(r))
            return dict_array
    except Exception as e:
        print(e)
        return None
