from sqlalchemy import create_engine, Column, Integer, String, Text
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base
import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")

if not DATABASE_URL:
    raise ValueError("DATABASE_URL not found in .env file. Please create the file and add the variable.")

engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class Analysis(Base):
    __tablename__ = "analyses"
    id = Column(Integer, primary_key=True, index=True)
    classification = Column(String, index=True)
    summary = Column(Text)
    decision_tree_json = Column(Text)

Base.metadata.create_all(bind=engine)

# This is the function that was missing from your old file
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()