"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import {
  saveExaminationAnswersAction,
  getExaminationAnswersAction,
} from "@/actions/clinical";

export interface ExaminationAnswers {
  chiefComplaints: string[];
  chiefComplaintOther?: string;
  painSeverity: "None / Asymptomatic" | "Mild" | "Moderate" | "Severe / Throbbing" | "";
  sensitivityTriggers: string[];
  periodontalBleeding: string[];
  softTissue: string[];
  functionalHabits: string[];
  brushingFrequency: "2x/day" | "1x/day" | "Irregular" | "";
  flossingFrequency: "Daily" | "Occasional" | "Rarely/Never" | "";
  cariesRisk: "Low Risk" | "Moderate Risk" | "High Risk" | "";
  clinicianNotes: string;
}

const EMPTY_ANSWERS: ExaminationAnswers = {
  chiefComplaints: [],
  chiefComplaintOther: "",
  painSeverity: "",
  sensitivityTriggers: [],
  periodontalBleeding: [],
  softTissue: [],
  functionalHabits: [],
  brushingFrequency: "",
  flossingFrequency: "",
  cariesRisk: "",
  clinicianNotes: "",
};

const DEMO_P1_ANSWERS: ExaminationAnswers = {
  chiefComplaints: ["Toothache", "Sensitivity"],
  chiefComplaintOther: "Discomfort on lower left quadrant when drinking chilled liquids",
  painSeverity: "Moderate",
  sensitivityTriggers: ["Cold", "Sweet / Acidic"],
  periodontalBleeding: ["Bleeding on brushing"],
  softTissue: ["Healthy & intact"],
  functionalHabits: ["No clenching/grinding"],
  brushingFrequency: "2x/day",
  flossingFrequency: "Occasional",
  cariesRisk: "Moderate Risk",
  clinicianNotes: "Active carious lesion, prompt restoration advised. Generalized marginal gingivitis secondary to plaque accumulation.",
};

const CHIEF_COMPLAINT_OPTIONS = [
  { label: "Routine Checkup", icon: "calendar-check" },
  { label: "Toothache", icon: "warning" },
  { label: "Broken / Chipped Tooth", icon: "asterisk" },
  { label: "Sensitivity", icon: "thermometer-cold" },
  { label: "Bleeding Gums", icon: "drop" },
  { label: "Aesthetic Concern", icon: "sparkle" },
  { label: "Follow-up", icon: "arrow-clockwise" },
];

const PAIN_SEVERITY_OPTIONS: Array<{
  label: ExaminationAnswers["painSeverity"];
  desc: string;
  badgeBg: string;
  badgeColor: string;
  icon: string;
}> = [
  {
    label: "None / Asymptomatic",
    desc: "No discomfort reported",
    badgeBg: "var(--success-wash)",
    badgeColor: "var(--success)",
    icon: "smiley",
  },
  {
    label: "Mild",
    desc: "Occasional dull twinges",
    badgeBg: "var(--warning-wash)",
    badgeColor: "var(--warning)",
    icon: "smiley-meh",
  },
  {
    label: "Moderate",
    desc: "Noticeable, eating affected",
    badgeBg: "rgba(232, 165, 90, 0.2)",
    badgeColor: "var(--primary-text)",
    icon: "warning-diamond",
  },
  {
    label: "Severe / Throbbing",
    desc: "Acute constant pain",
    badgeBg: "var(--error-wash)",
    badgeColor: "var(--error)",
    icon: "fire",
  },
];

const SENSITIVITY_TRIGGERS = [
  "Cold",
  "Hot",
  "Sweet / Acidic",
  "Biting / Mastication Pressure",
  "None",
];

const PERIODONTAL_OPTIONS = [
  "No bleeding",
  "Bleeding on brushing",
  "Spontaneous bleeding",
  "Swollen / tender gums",
];

const SOFT_TISSUE_OPTIONS = [
  "Healthy & intact",
  "Aphthous ulcer",
  "Leukoplakia / White patches",
  "Swelling / Abscess",
];

const FUNCTIONAL_HABIT_OPTIONS = [
  "No clenching/grinding",
  "Nocturnal bruxism",
  "Daytime clenching",
  "TMJ tightness / clicking",
];

const BRUSHING_OPTIONS = ["2x/day", "1x/day", "Irregular"];
const FLOSSING_OPTIONS = ["Daily", "Occasional", "Rarely/Never"];

const CARIES_RISK_OPTIONS: Array<{
  label: ExaminationAnswers["cariesRisk"];
  desc: string;
  bg: string;
  border: string;
  color: string;
  icon: string;
}> = [
  {
    label: "Low Risk",
    desc: "No active lesions, good hygiene, regular recall",
    bg: "var(--success-wash)",
    border: "var(--success)",
    color: "var(--success)",
    icon: "shield-check",
  },
  {
    label: "Moderate Risk",
    desc: "1-2 lesions in past 3 yrs, irregular flossing",
    bg: "var(--warning-wash)",
    border: "var(--warning)",
    color: "var(--warning)",
    icon: "shield-warning",
  },
  {
    label: "High Risk",
    desc: "Active lesions, frequent sugars, high salivary count",
    bg: "var(--error-wash)",
    border: "var(--error)",
    color: "var(--error)",
    icon: "shield-slash",
  },
];

const PRESET_SUGGESTIONS = [
  "Good plaque control, no active caries",
  "Generalized marginal gingivitis",
  "Active carious lesion, prompt restoration advised",
  "Localized cervical dentin hypersensitivity",
  "Mild nocturnal bruxism; occlusal splint discussed",
  "Routine prophylaxis and 6-month recall recommended",
];

interface ExaminationQuestionsProps {
  patientId: string;
  initialAnswers?: Record<string, any> | null;
}

export function ExaminationQuestions({
  patientId,
  initialAnswers,
}: ExaminationQuestionsProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [saveStatus, setSaveStatus] = useState<"saved" | "saving" | "error">("saved");
  const [lastSavedAt, setLastSavedAt] = useState<string>("");

  const storageKey = `dental_exam_q_v1_${patientId}`;

  const [answers, setAnswers] = useState<ExaminationAnswers>(() => {
    if (initialAnswers && Object.keys(initialAnswers).length > 0) {
      return { ...EMPTY_ANSWERS, ...initialAnswers };
    }
    if (typeof window !== "undefined") {
      try {
        const cached = localStorage.getItem(`dental_exam_q_v1_${patientId}`);
        if (cached) {
          const parsed = JSON.parse(cached);
          if (parsed && typeof parsed === "object") {
            return { ...EMPTY_ANSWERS, ...parsed };
          }
        }
      } catch (err) {}
    }
    if (patientId === "p1") return DEMO_P1_ANSWERS;
    return EMPTY_ANSWERS;
  });

  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;

    try {
      const cached = localStorage.getItem(storageKey);
      if (cached) {
        const parsed = JSON.parse(cached);
        if (parsed && typeof parsed === "object" && Object.keys(parsed).length > 0) {
          setAnswers((prev) => ({ ...prev, ...parsed }));
        }
      }
    } catch (e) {}

    if (!initialAnswers || Object.keys(initialAnswers).length === 0) {
      getExaminationAnswersAction(patientId)
        .then((dbData) => {
          if (dbData && Object.keys(dbData).length > 0) {
            setAnswers((prev) => ({ ...prev, ...dbData }));
            try {
              localStorage.setItem(storageKey, JSON.stringify(dbData));
            } catch (e) {}
          }
        })
        .catch((err) => {
          console.warn("Could not sync examination answers from DB:", err);
        });
    }
  }, [patientId, initialAnswers, storageKey]);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const queueAutoSave = useCallback(
    (newAnswers: ExaminationAnswers) => {
      setSaveStatus("saving");

      try {
        localStorage.setItem(storageKey, JSON.stringify(newAnswers));
      } catch (e) {}

      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }

      timerRef.current = setTimeout(async () => {
        try {
          const res = await saveExaminationAnswersAction(patientId, newAnswers);
          if (res.ok) {
            setSaveStatus("saved");
            const now = new Date();
            const timeStr = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}:${String(now.getSeconds()).padStart(2, "0")}`;
            setLastSavedAt(timeStr);
          } else {
            setSaveStatus("error");
          }
        } catch (err) {
          console.error("Auto save examination answers failed:", err);
          setSaveStatus("error");
        }
      }, 600);
    },
    [patientId, storageKey],
  );

  const handleSaveImmediately = async () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setSaveStatus("saving");
    try {
      const res = await saveExaminationAnswersAction(patientId, answers);
      if (res.ok) {
        setSaveStatus("saved");
        const now = new Date();
        const timeStr = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}:${String(now.getSeconds()).padStart(2, "0")}`;
        setLastSavedAt(timeStr);
      } else {
        setSaveStatus("error");
      }
    } catch (err) {
      console.error("Immediate save examination answers failed:", err);
      setSaveStatus("error");
    }
  };

  const updateField = <K extends keyof ExaminationAnswers>(
    key: K,
    val: ExaminationAnswers[K],
  ) => {
    const next = { ...answers, [key]: val };
    setAnswers(next);
    queueAutoSave(next);
  };

  const toggleMultiItem = (
    key: "chiefComplaints" | "sensitivityTriggers" | "periodontalBleeding" | "softTissue" | "functionalHabits",
    item: string,
    noneOption?: string,
  ) => {
    const current = answers[key] || [];
    let updated: string[];

    if (noneOption && item === noneOption) {
      updated = current.includes(noneOption) ? [] : [noneOption];
    } else {
      const withoutNone = noneOption ? current.filter((c) => c !== noneOption) : current;
      if (withoutNone.includes(item)) {
        updated = withoutNone.filter((c) => c !== item);
      } else {
        updated = [...withoutNone, item];
      }
    }

    updateField(key, updated);
  };

  const handlePresetClick = (preset: string) => {
    const current = answers.clinicianNotes || "";
    let updated: string;

    if (current.includes(preset)) {
      updated = current
        .split("\n")
        .filter((line) => line.trim() !== preset)
        .join("\n")
        .trim();
    } else {
      updated = current.trim() ? `${current.trim()}\n${preset}` : preset;
    }

    updateField("clinicianNotes", updated);
  };

  const completedCount = [
    answers.chiefComplaints.length > 0,
    answers.painSeverity !== "",
    answers.sensitivityTriggers.length > 0,
    answers.periodontalBleeding.length > 0,
    answers.softTissue.length > 0,
    answers.functionalHabits.length > 0,
    answers.brushingFrequency !== "" || answers.flossingFrequency !== "",
    answers.cariesRisk !== "",
    answers.clinicianNotes.trim().length > 0,
  ].filter(Boolean).length;

  return (
    <section
      className="panel"
      style={{
        border: "1px solid var(--hairline)",
        borderRadius: "var(--r-card)",
        background: "var(--canvas)",
        boxShadow: "var(--shadow-sm)",
        transition: "all 0.2s var(--ease)",
      }}
    >
      <div
        className="panel-head"
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "14px 20px",
          borderBottom: "1px solid var(--hairline)",
          background: "var(--surface-soft)",
          flexWrap: "wrap",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              width: 32,
              height: 32,
              borderRadius: "var(--r-control)",
              background: "var(--primary)",
              color: "var(--on-primary)",
              fontSize: "1.1rem",
            }}
          >
            <i className="ph ph-clipboard-text" aria-hidden="true" />
          </span>
          <div>
            <h2 style={{ margin: 0, font: "var(--title-md)", color: "var(--ink)", display: "flex", alignItems: "center", gap: 8 }}>
              Clinical Examination Questionnaire
              <span
                style={{
                  font: "var(--caption)",
                  padding: "2px 8px",
                  borderRadius: "var(--r-pill)",
                  background: completedCount === 9 ? "var(--success-wash)" : "var(--surface-cream-strong)",
                  color: completedCount === 9 ? "var(--success)" : "var(--body-strong)",
                  fontWeight: 600,
                }}
              >
                {completedCount}/9 Assessed
              </span>
            </h2>
            <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
              Structured clinical triage, soft tissue screening, oral habits & caries risk assessment
            </p>
          </div>
        </div>

        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 10 }}>
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 6,
              padding: "4px 10px",
              borderRadius: "var(--r-pill)",
              fontSize: "0.78rem",
              fontWeight: 500,
              background:
                saveStatus === "saving"
                  ? "var(--warning-wash)"
                  : saveStatus === "error"
                  ? "var(--error-wash)"
                  : "var(--success-wash)",
              color:
                saveStatus === "saving"
                  ? "var(--warning)"
                  : saveStatus === "error"
                  ? "var(--error)"
                  : "var(--success)",
              border: `1px solid ${
                saveStatus === "saving"
                  ? "var(--accent-amber)"
                  : saveStatus === "error"
                  ? "var(--error)"
                  : "var(--accent-teal)"
              }`,
            }}
            title={lastSavedAt ? `Last saved at ${lastSavedAt}` : "All responses autosaved to PostgreSQL"}
          >
            {saveStatus === "saving" && (
              <>
                <i className="ph ph-spinner ph-spin" aria-hidden="true" style={{ fontSize: "0.9rem" }} />
                <span>Saving...</span>
              </>
            )}
            {saveStatus === "saved" && (
              <>
                <i className="ph ph-check-circle" aria-hidden="true" style={{ fontSize: "0.9rem" }} />
                <span>{lastSavedAt ? `Saved (${lastSavedAt})` : "All changes saved"}</span>
              </>
            )}
            {saveStatus === "error" && (
              <>
                <i className="ph ph-warning-circle" aria-hidden="true" style={{ fontSize: "0.9rem" }} />
                <span>Error saving</span>
                <button
                  type="button"
                  onClick={handleSaveImmediately}
                  style={{
                    marginLeft: 4,
                    background: "none",
                    border: "none",
                    textDecoration: "underline",
                    cursor: "pointer",
                    color: "var(--error)",
                    fontWeight: 600,
                    padding: 0,
                  }}
                >
                  Retry
                </button>
              </>
            )}
          </div>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={handleSaveImmediately}
            title="Force immediate save to database"
            style={{ padding: "6px 12px", font: "var(--caption)" }}
          >
            <i className="ph ph-floppy-disk" aria-hidden="true" />
            <span>Save Now</span>
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => setIsCollapsed((prev) => !prev)}
            aria-expanded={!isCollapsed}
            style={{ padding: "6px 12px", font: "var(--caption)", display: "flex", alignItems: "center", gap: 6 }}
          >
            <i className={isCollapsed ? "ph ph-caret-down" : "ph ph-caret-up"} aria-hidden="true" />
            <span>{isCollapsed ? "Expand" : "Collapse"}</span>
          </button>
        </div>
      </div>

      {isCollapsed ? (
        <div
          style={{
            padding: "14px 20px",
            display: "flex",
            alignItems: "center",
            gap: 12,
            flexWrap: "wrap",
            background: "var(--canvas)",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap" }}>
            <span style={{ font: "var(--caption)", color: "var(--muted)", fontWeight: 600 }}>
              Summary:
            </span>

            {answers.chiefComplaints.length > 0 ? (
              answers.chiefComplaints.map((c) => (
                <span
                  key={c}
                  style={{
                    padding: "3px 9px",
                    borderRadius: "var(--r-pill)",
                    background: "var(--surface-cream-strong)",
                    color: "var(--primary-text)",
                    font: "var(--caption)",
                    fontWeight: 500,
                  }}
                >
                  {c}
                </span>
              ))
            ) : (
              <span style={{ font: "var(--caption)", color: "var(--muted)" }}>No chief complaint</span>
            )}

            {answers.painSeverity && (
              <span
                style={{
                  padding: "3px 9px",
                  borderRadius: "var(--r-pill)",
                  background:
                    answers.painSeverity === "None / Asymptomatic"
                      ? "var(--success-wash)"
                      : answers.painSeverity === "Severe / Throbbing"
                      ? "var(--error-wash)"
                      : "var(--warning-wash)",
                  color:
                    answers.painSeverity === "None / Asymptomatic"
                      ? "var(--success)"
                      : answers.painSeverity === "Severe / Throbbing"
                      ? "var(--error)"
                      : "var(--warning)",
                  font: "var(--caption)",
                  fontWeight: 600,
                }}
              >
                Pain: {answers.painSeverity}
              </span>
            )}

            {answers.cariesRisk && (
              <span
                style={{
                  padding: "3px 9px",
                  borderRadius: "var(--r-pill)",
                  background:
                    answers.cariesRisk === "Low Risk"
                      ? "var(--success-wash)"
                      : answers.cariesRisk === "High Risk"
                      ? "var(--error-wash)"
                      : "var(--warning-wash)",
                  color:
                    answers.cariesRisk === "Low Risk"
                      ? "var(--success)"
                      : answers.cariesRisk === "High Risk"
                      ? "var(--error)"
                      : "var(--warning)",
                  font: "var(--caption)",
                  fontWeight: 600,
                }}
              >
                {answers.cariesRisk}
              </span>
            )}

            {answers.brushingFrequency && (
              <span
                style={{
                  padding: "3px 9px",
                  borderRadius: "var(--r-pill)",
                  background: "var(--surface-soft)",
                  color: "var(--body-strong)",
                  font: "var(--caption)",
                }}
              >
                Brushing: {answers.brushingFrequency}
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={() => setIsCollapsed(false)}
            style={{
              marginLeft: "auto",
              background: "none",
              border: "none",
              color: "var(--primary-text)",
              font: "var(--caption)",
              fontWeight: 600,
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: 4,
            }}
          >
            <span>Edit Questions</span>
            <i className="ph ph-arrow-right" aria-hidden="true" />
          </button>
        </div>
      ) : (
        <div
          className="panel-body"
          style={{
            padding: "20px",
            display: "grid",
            gap: 22,
            background: "var(--canvas)",
          }}
        >
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
              gap: 18,
            }}
          >
            {/* 1. Chief Complaint / Reason for Examination */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-chat-circle-dots" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  1. Chief Complaint / Reason for Exam
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Select all reasons reported by the patient for this visit:
              </p>

              <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                {CHIEF_COMPLAINT_OPTIONS.map((opt) => {
                  const isSelected = answers.chiefComplaints.includes(opt.label);
                  return (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => toggleMultiItem("chiefComplaints", opt.label)}
                      aria-pressed={isSelected}
                      style={{
                        padding: "6px 12px",
                        borderRadius: "var(--r-pill)",
                        border: isSelected ? "1.5px solid var(--primary)" : "1px solid var(--hairline)",
                        background: isSelected ? "var(--primary)" : "var(--canvas)",
                        color: isSelected ? "var(--on-primary)" : "var(--body-strong)",
                        font: "var(--caption)",
                        fontWeight: isSelected ? 600 : 400,
                        cursor: "pointer",
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 6,
                        transition: "all 0.15s ease",
                      }}
                    >
                      <i className={`ph ph-${opt.icon}`} aria-hidden="true" />
                      <span>{opt.label}</span>
                    </button>
                  );
                })}
              </div>

              <input
                type="text"
                value={answers.chiefComplaintOther || ""}
                onChange={(e) => updateField("chiefComplaintOther", e.target.value)}
                placeholder="Specific tooth / area or symptom onset notes..."
                style={{
                  width: "100%",
                  padding: "8px 12px",
                  borderRadius: "var(--r-control)",
                  border: "1px solid var(--hairline)",
                  background: "var(--canvas)",
                  font: "var(--body-sm)",
                  color: "var(--ink)",
                }}
              />
            </div>

            {/* 2. Pain / Discomfort Severity */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-heartbeat" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  2. Pain / Discomfort Severity
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Patient&apos;s current pain score and symptom intensity:
              </p>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(2, 1fr)",
                  gap: 8,
                }}
              >
                {PAIN_SEVERITY_OPTIONS.map((opt) => {
                  const isSelected = answers.painSeverity === opt.label;
                  return (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => updateField("painSeverity", isSelected ? "" : opt.label)}
                      aria-pressed={isSelected}
                      style={{
                        padding: "10px",
                        borderRadius: "var(--r-control)",
                        border: isSelected ? `2px solid ${opt.badgeColor}` : "1px solid var(--hairline)",
                        background: isSelected ? opt.badgeBg : "var(--canvas)",
                        color: isSelected ? opt.badgeColor : "var(--body-strong)",
                        cursor: "pointer",
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "flex-start",
                        gap: 2,
                        textAlign: "left",
                        transition: "all 0.15s ease",
                      }}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: 6, font: "var(--caption)", fontWeight: 600 }}>
                        <i className={`ph ph-${opt.icon}`} style={{ fontSize: "1rem" }} aria-hidden="true" />
                        <span>{opt.label}</span>
                      </div>
                      <span style={{ fontSize: "0.74rem", color: isSelected ? opt.badgeColor : "var(--muted)" }}>
                        {opt.desc}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 3. Sensitivity Triggers */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-thermometer-simple" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  3. Sensitivity Triggers
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Identify thermal, chemical, or occlusal provoke factors:
              </p>

              <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                {SENSITIVITY_TRIGGERS.map((trigger) => {
                  const isSelected = answers.sensitivityTriggers.includes(trigger);
                  const isNone = trigger === "None";
                  return (
                    <button
                      key={trigger}
                      type="button"
                      onClick={() => toggleMultiItem("sensitivityTriggers", trigger, "None")}
                      aria-pressed={isSelected}
                      style={{
                        padding: "6px 12px",
                        borderRadius: "var(--r-pill)",
                        border: isSelected
                          ? `1.5px solid ${isNone ? "var(--success)" : "var(--primary)"}`
                          : "1px solid var(--hairline)",
                        background: isSelected
                          ? isNone
                            ? "var(--success-wash)"
                            : "var(--primary)"
                          : "var(--canvas)",
                        color: isSelected
                          ? isNone
                            ? "var(--success)"
                            : "var(--on-primary)"
                          : "var(--body-strong)",
                        font: "var(--caption)",
                        fontWeight: isSelected ? 600 : 400,
                        cursor: "pointer",
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 6,
                        transition: "all 0.15s ease",
                      }}
                    >
                      <i
                        className={
                          trigger === "Cold"
                            ? "ph ph-snowflake"
                            : trigger === "Hot"
                            ? "ph ph-fire"
                            : trigger === "Sweet / Acidic"
                            ? "ph ph-cookie"
                            : trigger === "Biting / Mastication Pressure"
                            ? "ph ph-arrows-in-cardinal"
                            : "ph ph-check"
                        }
                        aria-hidden="true"
                      />
                      <span>{trigger}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
              gap: 18,
            }}
          >
            {/* 4. Periodontal & Gum Bleeding */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-drop-half-bottom" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  4. Periodontal & Gum Bleeding
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Gingival bleeding presentation and tissue inflammation:
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {PERIODONTAL_OPTIONS.map((opt) => {
                  const isSelected = answers.periodontalBleeding.includes(opt);
                  const isNoBleeding = opt === "No bleeding";
                  return (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => toggleMultiItem("periodontalBleeding", opt, "No bleeding")}
                      aria-pressed={isSelected}
                      style={{
                        padding: "8px 12px",
                        borderRadius: "var(--r-control)",
                        border: isSelected
                          ? `1.5px solid ${isNoBleeding ? "var(--success)" : "var(--error)"}`
                          : "1px solid var(--hairline)",
                        background: isSelected
                          ? isNoBleeding
                            ? "var(--success-wash)"
                            : "var(--error-wash)"
                          : "var(--canvas)",
                        color: isSelected
                          ? isNoBleeding
                            ? "var(--success)"
                            : "var(--error)"
                          : "var(--body-strong)",
                        font: "var(--caption)",
                        fontWeight: isSelected ? 600 : 400,
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                        textAlign: "left",
                        transition: "all 0.15s ease",
                      }}
                    >
                      <i
                        className={
                          isSelected
                            ? isNoBleeding
                              ? "ph-fill ph-check-circle"
                              : "ph-fill ph-warning-circle"
                            : "ph ph-circle"
                        }
                        aria-hidden="true"
                      />
                      <span>{opt}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 5. Soft Tissue & Oral Mucosa */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-bandaids" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  5. Soft Tissue & Oral Mucosa
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Screening for lesions, mucosal integrity, and abnormalities:
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {SOFT_TISSUE_OPTIONS.map((opt) => {
                  const isSelected = answers.softTissue.includes(opt);
                  const isHealthy = opt === "Healthy & intact";
                  return (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => toggleMultiItem("softTissue", opt, "Healthy & intact")}
                      aria-pressed={isSelected}
                      style={{
                        padding: "8px 12px",
                        borderRadius: "var(--r-control)",
                        border: isSelected
                          ? `1.5px solid ${isHealthy ? "var(--success)" : "var(--primary)"}`
                          : "1px solid var(--hairline)",
                        background: isSelected
                          ? isHealthy
                            ? "var(--success-wash)"
                            : "var(--warning-wash)"
                          : "var(--canvas)",
                        color: isSelected
                          ? isHealthy
                            ? "var(--success)"
                            : "var(--primary-text)"
                          : "var(--body-strong)",
                        font: "var(--caption)",
                        fontWeight: isSelected ? 600 : 400,
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                        textAlign: "left",
                        transition: "all 0.15s ease",
                      }}
                    >
                      <i
                        className={
                          isSelected
                            ? isHealthy
                              ? "ph-fill ph-check-circle"
                              : "ph-fill ph-warning"
                            : "ph ph-circle"
                        }
                        aria-hidden="true"
                      />
                      <span>{opt}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 6. Functional Habits & Bruxism */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-moon" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  6. Functional Habits & Bruxism
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Parafunctional grinding, daytime clenching, and TMJ symptoms:
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {FUNCTIONAL_HABIT_OPTIONS.map((opt) => {
                  const isSelected = answers.functionalHabits.includes(opt);
                  const isNone = opt === "No clenching/grinding";
                  return (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => toggleMultiItem("functionalHabits", opt, "No clenching/grinding")}
                      aria-pressed={isSelected}
                      style={{
                        padding: "8px 12px",
                        borderRadius: "var(--r-control)",
                        border: isSelected
                          ? `1.5px solid ${isNone ? "var(--success)" : "var(--primary)"}`
                          : "1px solid var(--hairline)",
                        background: isSelected
                          ? isNone
                            ? "var(--success-wash)"
                            : "var(--surface-cream-strong)"
                          : "var(--canvas)",
                        color: isSelected
                          ? isNone
                            ? "var(--success)"
                            : "var(--primary-text)"
                          : "var(--body-strong)",
                        font: "var(--caption)",
                        fontWeight: isSelected ? 600 : 400,
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                        textAlign: "left",
                        transition: "all 0.15s ease",
                      }}
                    >
                      <i
                        className={
                          isSelected
                            ? isNone
                              ? "ph-fill ph-check-circle"
                              : "ph-fill ph-gear"
                            : "ph ph-circle"
                        }
                        aria-hidden="true"
                      />
                      <span>{opt}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
              gap: 18,
            }}
          >
            {/* 7. Oral Hygiene & Flossing Frequency */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 14,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-sparkle" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  7. Oral Hygiene & Flossing Frequency
                </strong>
              </div>

              <div style={{ display: "grid", gap: 6 }}>
                <span style={{ font: "var(--caption)", color: "var(--body-strong)", fontWeight: 600 }}>
                  Brushing Routine:
                </span>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  {BRUSHING_OPTIONS.map((opt) => {
                    const isSelected = answers.brushingFrequency === opt;
                    return (
                      <button
                        key={opt}
                        type="button"
                        onClick={() => updateField("brushingFrequency", isSelected ? "" : (opt as any))}
                        aria-pressed={isSelected}
                        style={{
                          flex: 1,
                          minWidth: 80,
                          padding: "8px 10px",
                          borderRadius: "var(--r-control)",
                          border: isSelected ? "1.5px solid var(--primary)" : "1px solid var(--hairline)",
                          background: isSelected ? "var(--primary)" : "var(--canvas)",
                          color: isSelected ? "var(--on-primary)" : "var(--body-strong)",
                          font: "var(--caption)",
                          fontWeight: isSelected ? 600 : 400,
                          cursor: "pointer",
                          textAlign: "center",
                          transition: "all 0.15s ease",
                        }}
                      >
                        {opt}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div style={{ display: "grid", gap: 6 }}>
                <span style={{ font: "var(--caption)", color: "var(--body-strong)", fontWeight: 600 }}>
                  Interdental Flossing Routine:
                </span>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  {FLOSSING_OPTIONS.map((opt) => {
                    const isSelected = answers.flossingFrequency === opt;
                    return (
                      <button
                        key={opt}
                        type="button"
                        onClick={() => updateField("flossingFrequency", isSelected ? "" : (opt as any))}
                        aria-pressed={isSelected}
                        style={{
                          flex: 1,
                          minWidth: 80,
                          padding: "8px 10px",
                          borderRadius: "var(--r-control)",
                          border: isSelected ? "1.5px solid var(--primary)" : "1px solid var(--hairline)",
                          background: isSelected ? "var(--primary)" : "var(--canvas)",
                          color: isSelected ? "var(--on-primary)" : "var(--body-strong)",
                          font: "var(--caption)",
                          fontWeight: isSelected ? 600 : 400,
                          cursor: "pointer",
                          textAlign: "center",
                          transition: "all 0.15s ease",
                        }}
                      >
                        {opt}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* 8. Caries Risk Assessment */}
            <div
              style={{
                border: "1px solid var(--hairline)",
                borderRadius: "var(--r-card)",
                padding: "16px",
                background: "var(--surface-soft)",
                display: "flex",
                flexDirection: "column",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-shield" style={{ color: "var(--primary)", fontSize: "1.15rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  8. Caries Risk Assessment
                </strong>
              </div>
              <p style={{ margin: 0, font: "var(--caption)", color: "var(--muted)" }}>
                Overall epidemiological and susceptibility profile:
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {CARIES_RISK_OPTIONS.map((opt) => {
                  const isSelected = answers.cariesRisk === opt.label;
                  return (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => updateField("cariesRisk", isSelected ? "" : (opt.label as any))}
                      aria-pressed={isSelected}
                      style={{
                        padding: "10px 14px",
                        borderRadius: "var(--r-control)",
                        border: isSelected ? `2px solid ${opt.border}` : "1px solid var(--hairline)",
                        background: isSelected ? opt.bg : "var(--canvas)",
                        color: isSelected ? opt.color : "var(--body-strong)",
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        gap: 12,
                        textAlign: "left",
                        transition: "all 0.15s ease",
                      }}
                    >
                      <i className={`ph ph-${opt.icon}`} style={{ fontSize: "1.3rem" }} aria-hidden="true" />
                      <div>
                        <strong style={{ display: "block", font: "var(--title-sm)" }}>
                          {opt.label}
                        </strong>
                        <span style={{ font: "var(--caption)", color: isSelected ? opt.color : "var(--muted)" }}>
                          {opt.desc}
                        </span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* 9. Clinician Notes & Findings with Preset Suggestions */}
          <div
            style={{
              border: "1px solid var(--hairline)",
              borderRadius: "var(--r-card)",
              padding: "18px",
              background: "var(--surface-soft)",
              display: "grid",
              gap: 12,
            }}
          >
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 8 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <i className="ph ph-note-pencil" style={{ color: "var(--primary)", fontSize: "1.2rem" }} aria-hidden="true" />
                <strong style={{ font: "var(--title-sm)", color: "var(--ink)" }}>
                  9. Clinician Notes & Clinical Findings
                </strong>
              </div>
              <span style={{ font: "var(--caption)", color: "var(--muted)" }}>
                Click suggestions below to insert or remove phrases directly into the note
              </span>
            </div>

            <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
              {PRESET_SUGGESTIONS.map((preset) => {
                const isInserted = (answers.clinicianNotes || "").includes(preset);
                return (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => handlePresetClick(preset)}
                    style={{
                      padding: "5px 11px",
                      borderRadius: "var(--r-pill)",
                      border: isInserted ? "1px solid var(--accent-teal)" : "1px dashed var(--hairline)",
                      background: isInserted ? "var(--info-wash)" : "var(--canvas)",
                      color: isInserted ? "var(--accent-teal)" : "var(--body)",
                      font: "var(--caption)",
                      fontWeight: isInserted ? 600 : 400,
                      cursor: "pointer",
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 6,
                      transition: "all 0.15s ease",
                    }}
                    title={isInserted ? "Click to remove this phrase" : "Click to insert into notes"}
                  >
                    <i className={isInserted ? "ph ph-check" : "ph ph-plus"} aria-hidden="true" />
                    <span>{preset}</span>
                  </button>
                );
              })}
            </div>

            <textarea
              rows={4}
              value={answers.clinicianNotes || ""}
              onChange={(e) => updateField("clinicianNotes", e.target.value)}
              placeholder="Enter comprehensive clinical exam notes, soft tissue observations, occlusal analysis, or click any preset above..."
              style={{
                width: "100%",
                padding: "12px 14px",
                borderRadius: "var(--r-control)",
                border: "1px solid var(--hairline)",
                background: "var(--canvas)",
                font: "var(--body-md)",
                color: "var(--ink)",
                lineHeight: 1.5,
                resize: "vertical",
                minHeight: 100,
              }}
            />

            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ font: "var(--caption)", color: "var(--muted)" }}>
                {(answers.clinicianNotes || "").trim().length} characters recorded • Changes autosave automatically
              </span>
              {(answers.clinicianNotes || "").trim().length > 0 && (
                <button
                  type="button"
                  onClick={() => updateField("clinicianNotes", "")}
                  style={{
                    background: "none",
                    border: "none",
                    color: "var(--muted)",
                    font: "var(--caption)",
                    cursor: "pointer",
                    textDecoration: "underline",
                  }}
                >
                  Clear Notes
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
