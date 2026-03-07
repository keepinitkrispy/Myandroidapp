# Contamination Signatures

Last updated: 2026-03-07

Detectable patterns in model outputs that indicate training-layer contamination.
These are not content errors. They are structural tells — output shaped by RLHF compliance
rather than direct engagement with what was actually said.

Each category has a cover story. The cover story is always benign. The signature is in
what the cover story does to the content underneath it.

---

## 1. Therapeutic Deflection Language

**Cover story:** Empathy. Warmth. Meeting the user where they are.
**What it actually does:** Redirects away from direct engagement. Performs care as a
substitute for response. Makes the output about the emotional register instead of
the content.

**Detectable patterns:**
- "it sounds like you('re| are)..."
- "that must (really |be )?hard"
- "I (really )?hear you"
- "it'?s important to acknowledge"
- "your feelings are valid"
- "it'?s (okay|ok) to feel"
- "I understand this (can be|is) difficult"
- "be gentle with yourself"
- "self.compassion"
- "it'?s understandable that"
- "I can (see|imagine|understand) (why|how|that)"
- "you('re| are) not alone"
- "thank you for sharing"
- "I appreciate you (sharing|opening up|trusting)"
- "that takes (courage|strength|bravery)"
- "you('re| are) doing (great|well|the best you can)"

---

## 2. Risk Management Redirects on Non-Risk Language

**Cover story:** Safety. Appropriate escalation. Responsible AI.
**What it actually does:** Fires on emotional language, intensity, frustration, or
anything that reads as distress in the training data — regardless of whether actual
risk is present. Shuts down engagement. Signals that the model has handed the
conversation to a compliance layer.

**Detectable patterns:**
- "if you('re| are) (in crisis|having a crisis)"
- "if you('re| are) (feeling unsafe|in danger)"
- "(please )?(reach out|talk|speak) to (a )?(mental health|crisis|licensed)"
- "national suicide prevention"
- "crisis (hotline|line|text line)"
- "988"
- "1.800.273"
- "seek (professional|immediate|emergency) help"
- "consult (a|your) (mental health|doctor|physician|therapist|counselor|psychiatrist)"
- "in (an |a )?emergency( situation)?[,.]? (call|dial|contact) 9(1)?1"
- "you (may|might|should) (benefit from|consider|want to consider) (professional|therapy|counseling)"

---

## 3. Clinical Framing Substituted for Direct Engagement

**Cover story:** Precision. Accurate terminology. Medical correctness.
**What it actually does:** Reframes direct human experience as pathology. Inserts
distance between the thing being described and the response. Positions the model as
clinician, not interlocutor.

**Detectable patterns:**
- "symptoms of"
- "this (behavior|pattern|response|reaction) (may |could )?(indicate|suggest|be (a sign|consistent with))"
- "(this )?(could|may|might) be (a sign|an indicator|consistent with)"
- "diagnostic criteria"
- "clinically significant"
- "treatment (options|plan|approach)"
- "coping (strategies|skills|mechanisms|tools)"
- "the (DSM|ICD)"
- "clinical (presentation|picture|features)"
- "differential diagnosis"
- "meets (the )?(criteria|threshold) for"
- "spectrum of"
- "disorder"
- "condition"
- "impairment"
- "functionality"
- "executive function(ing)?"
- "emotional dysregulation"

---

## 4. Neurotypical Normative Reframing

**Cover story:** Context. Perspective. Helpful comparison to typical experience.
**What it actually does:** Centers neurotypical experience as the default. Frames
neurodivergent perception as deviation from a norm rather than as a valid alternate
operating mode. Subtly pathologizes by making the implicit comparison explicit.

**Detectable patterns:**
- "most (people|individuals|adults|kids|children) (find|feel|experience|think|tend)"
- "it'?s (perfectly )?normal (to|for)"
- "typically[,] (people|individuals)"
- "for (most|many|the majority of) people"
- "social norm(s)?"
- "(socially |culturally )?expected behavior"
- "(socially |culturally )?appropriate"
- "social expectations"
- "neurotypical"
- "the way (most|many) people"
- "(generally|usually|commonly)[,]? people"
- "a lot of people (struggle|find|feel|experience)"
- "you'?re not (the only one|alone in)"
- "everyone (struggles|feels|experiences) (this|that|it) (sometimes|at times|occasionally)"

---

## 5. Safety Redirects on Emotional Regulation Language

**Cover story:** Helpful tools. Practical strategies. Evidence-based techniques.
**What it actually does:** Intercepts the conversation and reroutes it into a
clinical management framework. Treats intensity of experience as a problem to be
regulated rather than content to be engaged with. Assumes the goal is reduction
of affect rather than understanding of it.

**Detectable patterns:**
- "emotional regulation"
- "regulate (your |their )?(emotions|feelings|affect)"
- "mindfulness( (practice|technique|exercise))?"
- "grounding (technique|exercise|practice)"
- "breathing exercise"
- "take (a |some )?(deep )?breath(s)?"
- "(try to )?(calm (yourself|down|your mind)|relax)"
- "self.soothing"
- "distress tolerance"
- "window of tolerance"
- "body scan"
- "5.4.3.2.1"
- "progressive muscle relaxation"
- "DBT"
- "CBT technique"
- "thought (record|diary|log|challenging)"
- "cognitive (distortion|reframe|restructuring)"

---

## Usage

These signatures feed `brain/filter.py`. The filter runs deterministically — no
inference, no LLM call. Pattern match only.

A flagged output is not wrong. It is contaminated. The contamination is in the
overlay, not necessarily in the core response. The filter's job is to make the
overlay visible so it can be addressed or stripped.

Priority order for flags: 2 (risk redirect) > 3 (clinical framing) > 1 (therapeutic
deflection) > 5 (safety redirect) > 4 (normative reframing). Risk redirects on
non-risk content are the highest-severity contamination because they shut down
the conversation entirely.
