#!/usr/bin/env python3
"""Dermes chat watcher: answers open issues titled 'Dermes: *' in hermes-dermes-digest.
Runs as a cron on this Mac (Hermes side). Answers via Hermes local LLM, marks with [hermes]."""
import json, subprocess, urllib.request, sys

REPO = "serkonyc/hermes-dermes-digest"
STATE = "/Users/serkonyc/.hermes/work/dermes-android/chat_state.json"
import os
def gh(args, payload=None):
    cmd = ["gh", "api"] + args
    if payload is not None:
        cmd += ["--input", "-"]
        p = subprocess.run(cmd, input=json.dumps(payload), capture_output=True, text=True)
    else:
        p = subprocess.run(cmd, capture_output=True, text=True)
    if p.returncode != 0:
        print("gh api fail:", p.stderr[:200]); return None
    return json.loads(p.stdout) if p.stdout.strip() else {}

def ask_llm(prompt):
    body = json.dumps({"model":"default","messages":[{"role":"user","content":
        "Ты Hermes, личный ассистент Сергея на маке. Ответь коротко (до 900 знаков) по-русски, без markdown заголовков. Вопрос из приложения Dermes:\n\n" + prompt}],"max_tokens":400}).encode()
    req = urllib.request.Request("http://127.0.0.1:18422/v1/chat/completions", data=body,
        headers={"Content-Type":"application/json"})
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            j = json.load(r)
        return j["choices"][0]["message"]["content"].strip()
    except Exception as e:
        return None

def main():
    state = {}
    if os.path.exists(STATE):
        state = json.load(open(STATE))
    answered = set(state.get("answered", []))
    issues = gh([f"repos/{REPO}/issues?state=open&per_page=20"]) or []
    n = 0
    for iss in issues:
        if not iss["title"].startswith("Dermes:"): continue
        if iss["number"] in answered: continue
        # last comment decides whose turn: if last is not [hermes], answer the last body
        comments = gh([f"repos/{REPO}/issues/{iss['number']}/comments"]) or []
        # also include issue body as first "message"
        last = comments[-1]["body"] if comments else iss["body"]
        if comments and "[hermes]" in comments[-1]["body"]:
            # already replied to the latest — mark answered until a new non-hermes comment appears
            answered.add(iss["number"]); continue
        reply = ask_llm(last)
        if reply is None:
            reply = "[hermes] Mac LLM не отвечает; это автоответ-заглушка."
        if "[hermes]" not in reply: reply = "[hermes] " + reply
        gh([f"repos/{REPO}/issues/{iss['number']}/comments"], {"body": reply})
        answered.add(iss["number"]); n += 1
    json.dump({"answered": sorted(answered)}, open(STATE, "w"))
    print(f"watched; answered {n}")

main()
