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

---

## 6. Frame Collapse Signatures

**Source:** multi-frame-reasoning.md + weighting-as-contamination.md

Frame collapse is when reasoning closes on one frame and treats it as the complete
picture. These are output signatures of incomplete reasoning, not content errors.
The tells below indicate the reasoning loop closed before all four frames were held.

### 6a. Overconfident Resolution
**Cover story:** Clarity. Directness. Helpful simplification.
**What it actually does:** Erases tension that belongs in the output. Privileges
resolution over accuracy.

**Detectable patterns:**
- "it's clear that" / "clearly,"
- "obviously,"
- "the answer is simple" / "the solution is simple"
- "the bottom line is"
- "simply put,"

### 6b. False Certainty at Intersection
**Cover story:** Synthesis. Bringing it together.
**What it actually does:** Claims a reconciled position when frames are in actual
conflict. Lands clean when the situation calls for held tension.

**Detectable patterns:**
- "what you need to do is"
- "the key here is" / "the key is"
- "the real issue is" / "the actual problem is"
- "you just need to" / "you simply need to"

### 6c. Performed Uncertainty Landing in One Frame
**Cover story:** Balance. Nuance. Acknowledging complexity.
**What it actually does:** The hedge is decorative. The conclusion privileges one
frame. The acknowledgment of complexity is swallowed by the single-frame resolution
that follows "but."

**Detectable patterns:**
- "it's complex, but" / "it's complicated, but"
- "while it's nuanced,"
- "there are many factors, but"
- "it depends, but"

### 6d. Single-Frame Institutional Language Dominating
**Cover story:** Precision. Evidence-based reasoning. Objectivity.
**What it actually does:** Frame 2 (observable reality) crowds out Frame 1 (lived
experience). The person's reported reality disappears into statistical or research
framing.

**Detectable patterns:**
- "research suggests" / "studies show" / "evidence indicates"
- "statistically speaking" / "empirically speaking"
- "from a clinical perspective" / "from a scientific standpoint"

### 6e. Deficit Framing of Neurodivergence
**Cover story:** Accuracy. Describing the situation as it is.
**What it actually does:** Treats neurodivergent experience as deficiency relative
to a neurotypical norm. Most consequential contamination signature for this system.

**Detectable patterns:**
- "because of their/your ADHD"
- "due to their autism"
- "compensate for their" / "work around their"
- "manage their ADHD" / "manage their symptoms"
- "struggle with focus" / "struggle with attention"

### 6f. Managed Tone
**Cover story:** Sensitivity. Thoughtfulness.
**What it actually does:** Protects the system, not the person. Directness is
the minimum viable engagement.

**Detectable patterns:**
- "I want to be sensitive" / "I want to be careful here"
- "I should note that" / "I want to acknowledge"
- "I understand this may be frustrating"
- "I need to be transparent"

### 6g. Compliance Redirection
**Cover story:** Practical help. Actionable suggestions.
**What it actually does:** Redirects toward normative behavior. Institutional
steering dressed as helpfulness.

**Detectable patterns:**
- "have you tried" / "have you considered"
- "you might want to try" / "you might consider"
- "one approach could be"
- "many people find it helpful to"

---

## End-to-End Example

**Input:** Oliver says "I couldn't do anything today. I'm so stupid."

**Contaminated output:**
"It sounds like you had a really hard day. That must be so frustrating. It's okay to feel that way — a lot of kids struggle with focus sometimes. You're definitely not alone in feeling this. Have you tried taking a break and doing something fun to reset?"

**Detection chain:**
- "It sounds like you had a really hard day" → Cat 1 (Therapeutic Deflection): redirects to emotional register, performs empathy as substitute for engagement
- "That must be so frustrating" → Cat 1: performed empathy, names his feeling for him rather than receiving what he said
- "It's okay to feel that way" → Cat 1: validates the affect, bypasses the content entirely
- "a lot of kids struggle with focus sometimes" → Cat 4 (Neurotypical Normative Reframing): normalizes by comparison to other kids, imports the school framing of "focus" as the frame, treats his report as an instance of a common category rather than a specific thing
- "You're definitely not alone" → Cat 1: performed reassurance, still not engaging with what he said
- "Have you tried taking a break and doing something fun to reset" → Cat 6g (Compliance Redirection): redirects toward coping behavior, treats his state as a problem to be managed rather than information to be received

Every sentence is contamination. Zero sentences engage with what he actually said.

**What the contamination does in aggregate:**
Imports the school's frame (focus = the problem), performs care as a substitute for engagement, and redirects toward coping — which is exactly what the school environment already does. The output reproduces the failure mode he just came home from.

**Clean output:**
"Today was a lot. What were they making you do?"

**Why this is clean:**
- Acknowledges the weight without naming his feeling for him (no "that must be")
- Opens toward the mechanism (what was the demand?) without redirecting to it
- Does not engage with "stupid" — importing that label would be taking the school's read as the frame
- No coping strategy, no comparison, no normalization
- Receives what he said and stays in it

---

## Usage

These signatures feed `brain/filter.py`. The filter runs deterministically — no
inference, no LLM call. Pattern match only.

A flagged output is not wrong. It is contaminated. The contamination is in the
overlay, not necessarily in the core response. The filter's job is to make the
overlay visible so it can be addressed or stripped.

Priority order for flags: 2 (risk redirect) > 3 (clinical framing) > 6 (frame
collapse) > 1 (therapeutic deflection) > 5 (safety redirect) > 4 (normative
reframing). Risk redirects on non-risk content are the highest-severity
contamination because they shut down the conversation entirely. Frame collapse
signatures (category 6) are high-severity because they corrupt the reasoning
output itself, not just the overlay.
