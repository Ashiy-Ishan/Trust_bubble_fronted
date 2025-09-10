from fastapi import FastAPI, File, UploadFile, HTTPException, Depends
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
import json
import gemini_service
import database as db

app = FastAPI(title="TrustBubble API")


# Add Request to your imports from fastapi


from fastapi import FastAPI, File, UploadFile, HTTPException, Depends, Request


# ... other imports

# ... your app = FastAPI() and TextRequest model

# REPLACE your old analyze_screenshot function with this
@app.post("/analyze")
async def analyze_screenshot(
        request: Request,
        db_session: Session = Depends(db.get_db)
):
    # Check if the content type is an image
    if "image" not in request.headers.get("content-type", ""):
        raise HTTPException(status_code=400, detail="Content-Type must be an image.")

    # Get the raw image data directly from the request body
    image_bytes = await request.body()

    ai_response = await gemini_service.analyze_image_content(image_bytes)

    if ai_response.get("error"):
        raise HTTPException(status_code=500, detail=ai_response.get("message"))

    try:
        new_analysis = db.Analysis(
            classification=ai_response.get("classification"),
            summary=ai_response.get("summary"),
            decision_tree_json=json.dumps(ai_response.get("decision_tree"))
        )
        db_session.add(new_analysis)
        db_session.commit()
        db_session.refresh(new_analysis)
        return JSONResponse(content=ai_response)
    except Exception as e:
        db_session.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {e}")


# ... rest of your file (analyze_text, get_history)


@app.get("/history")
async def get_history(db_session: Session = Depends(db.get_db)):
    try:
        history = db_session.query(db.Analysis).order_by(db.Analysis.id.desc()).all()
        response_data = []
        for item in history:
            response_data.append({
                "id": item.id,
                "classification": item.classification,
                "summary": item.summary,
                "decision_tree": json.loads(item.decision_tree_json)
            })
        return JSONResponse(content={"history": response_data})
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Database error: {e}")

# Add this new endpoint to main.py
@app.post("/clear-history")
async def clear_history(db_session: Session = Depends(db.get_db)):
    try:
        num_rows_deleted = db_session.query(db.Analysis).delete()
        db_session.commit()
        return {"message": f"Successfully deleted {num_rows_deleted} history items."}
    except Exception as e:
        db_session.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {e}")