import os
import google.generativeai as genai
from dotenv import load_dotenv
from PIL import Image
import io
import json

load_dotenv()

try:
    genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
except Exception as e:
    print(f"Failed to configure Gemini API. Check your GEMINI_API_KEY in the .env file. Error: {e}")


SYSTEM_PROMPT = """
You are a meticulous AI analyst. Your task is to analyze the image and explain your reasoning by following a decision path.

For each step in your reasoning, you will be presented with a question. You must choose "yes" or "no" and provide a brief, one-sentence "reason" for your choice that is directly related to the content of the image.

⚠️ **IMPORTANT CLASSIFICATION RULE:** If a claim is found to be misleading, false, a known conspiracy, or if the content is otherwise unsafe (violent, hateful), it **MUST** be classified as "Harmful".

After your analysis, you must provide your response in a structured JSON format.

1.  **Classification**: "Good" or "Harmful".
2.  **Summary**: A one-sentence summary of your final conclusion.
3.  **Decision Path**: A structured path of your reasoning.
    - Each step must have a "question".
    - The "yes" and "no" branches must contain a "reason" and a "next_step".
    - The "reason" MUST explain why you chose that path based on the image content.
    - The "next_step" leads to another question or a final "decision".

Strictly adhere to the following JSON output format.

**Example for a screenshot of a news article:**
{
  "classification": "Good",
  "summary": "The content is a standard news article from a reputable source with no harmful elements.",
  "decision_path": {
    "question": "Does the image contain a factual claim or headline?",
    "yes": {
      "reason": "Yes, the image contains a headline from CNN about Elon Musk.",
      "next_step": {
        "question": "Is the source reputable and is the content presented neutrally?",
        "yes": {
          "reason": "Yes, CNN is a major news organization and the content appears to be a standard news report.",
          "next_step": {
            "decision": "Classified as Good as it is a neutral article from a reputable source."
          }
        },
        "no": {
          "reason": null,
          "next_step": null
        }
      }
    },
    "no": {
      "reason": null,
      "next_step": null
    }
  }
}

**Final JSON Output:**
"""


async def analyze_image_content(image_bytes: bytes):
    try:
        model = genai.GenerativeModel("gemini-2.5-pro")
        image = Image.open(io.BytesIO(image_bytes))
        response = model.generate_content([SYSTEM_PROMPT, image])
        cleaned_text = response.text.strip().replace("```json", "").replace("```", "")
        json_response = json.loads(cleaned_text)
        return json_response
    except Exception as e:
        print(f"Error processing Gemini API response: {e}")
        return {
            "error": True,
            "message": "Failed to get a valid analysis from the AI."
        }