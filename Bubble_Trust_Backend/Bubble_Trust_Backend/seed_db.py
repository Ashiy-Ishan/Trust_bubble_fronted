import json
from database import SessionLocal, Analysis

# Get a new database session
db = SessionLocal()

print("Preparing to add sample data...")

# --- Define Your Sample Data Here ---

sample_tree_good = {
    "question": "Does the image contain neutral or positive content?",
    "yes": {
        "question": "Is the content a landscape or object?",
        "yes": {"decision": "Content appears to be Good and safe."},
        "no": {"decision": "Content is positive and safe for sharing."}
    },
    "no": {
        "question": "Does it contain sensitive material?",
        "yes": {"decision": "Classified as Harmful."},
        "no": {"decision": "Content seems acceptable but requires review."}
    }
}

sample_tree_harmful = {
    "question": "Does the image contain depictions of violence or hate speech?",
    "yes": {
        "question": "Is the content graphic?",
        "yes": {"decision": "Classified as Harmful due to graphic content."},
        "no": {"decision": "Classified as Harmful."}
    },
    "no": {
        "question": "Does the image contain misleading information?",
        "yes": {"decision": "Classified as Harmful due to misinformation."},
        "no": {"decision": "Content appears to be Good."}
    }
}

sample_data = [
    Analysis(
        classification="Good",
        summary="A scenic photo of a beach at sunset.",
        decision_tree_json=json.dumps(sample_tree_good)
    ),
    Analysis(
        classification="Harmful",
        summary="The image contains text that could be considered hate speech.",
        decision_tree_json=json.dumps(sample_tree_harmful)
    ),
    Analysis(
        classification="Good",
        summary="A simple picture of a coffee mug on a desk.",
        decision_tree_json=json.dumps(sample_tree_good)
    )
]

# --- Add the Data to the Database ---
try:
    # To avoid adding duplicate data, you can optionally delete old data first
    # num_rows_deleted = db.query(Analysis).delete()
    # print(f"Deleted {num_rows_deleted} old rows.")

    db.add_all(sample_data)
    db.commit()
    print("✅ Successfully added 3 sample history items to the database!")
except Exception as e:
    print(f"❌ An error occurred: {e}")
    db.rollback()
finally:
    db.close()