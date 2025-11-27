from comet import download_model, load_from_checkpoint
import json


model_path = download_model("Unbabel/wmt22-cometkiwi-da")
model = load_from_checkpoint(model_path)


def evaluate_comet_for_json(json_path: str) -> float:
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    comet_input = []

    for page in data["pages"]:
        mt_block = " ".join(page["sentences"])
        src_block = " ".join(page["translations"])

        comet_input.append({
            "src": src_block,
            "mt": mt_block
        })

    result = model.predict(comet_input, gpus=0)

    return result["system_score"]


def run():
    score_A = evaluate_comet_for_json("translated_A.json")
    score_B = evaluate_comet_for_json("translated_B.json")

    print(f"Avg score for prompt A: {score_A:.6f}")
    print(f"Avg score for prompt B: {score_B:.6f}")



if __name__ == "__main__":
    run()