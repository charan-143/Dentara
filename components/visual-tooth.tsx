"use client";

import React, { useId } from "react";

export interface VisualToothProps {
  toothNum: number;
  fdi: string;
  name: string;
  condition: string;
  isSelected: boolean;
  notes?: string;
  size?: "sm" | "md" | "lg";
  onClick?: () => void;
}

export type ToothAnatomyType = "molar" | "premolar" | "canine" | "incisor";

/**
 * Returns tooth anatomical classification based on universal tooth number (1-32).
 */
export function getToothAnatomyType(toothNum: number): ToothAnatomyType {
  // Molars: 1, 2, 3, 14, 15, 16, 17, 18, 19, 30, 31, 32
  if (
    (toothNum >= 1 && toothNum <= 3) ||
    (toothNum >= 14 && toothNum <= 19) ||
    (toothNum >= 30 && toothNum <= 32)
  ) {
    return "molar";
  }
  // Premolars: 4, 5, 12, 13, 20, 21, 28, 29
  if (
    toothNum === 4 ||
    toothNum === 5 ||
    toothNum === 12 ||
    toothNum === 13 ||
    toothNum === 20 ||
    toothNum === 21 ||
    toothNum === 28 ||
    toothNum === 29
  ) {
    return "premolar";
  }
  // Canines: 6, 11, 22, 27
  if (toothNum === 6 || toothNum === 11 || toothNum === 22 || toothNum === 27) {
    return "canine";
  }
  // Incisors: 7, 8, 9, 10, 23, 24, 25, 26
  return "incisor";
}

/**
 * Normalizes condition string into one of the clinical presets or custom.
 */
export function normalizeCondition(condition: string): {
  key: "sound" | "caries" | "restored" | "crown" | "implant" | "custom";
  label: string;
  badgeBg: string;
  badgeText: string;
  customColor?: string;
} {
  const c = condition.toLowerCase().trim();

  if (c === "sound" || c === "healthy" || c === "intact" || !c) {
    return {
      key: "sound",
      label: "Sound",
      badgeBg: "var(--surface-soft, #f5f0e8)",
      badgeText: "var(--body-strong, #252523)",
    };
  }
  if (c.includes("caries") || c.includes("decay") || c.includes("cavity")) {
    return {
      key: "caries",
      label: "Caries",
      badgeBg: "var(--error-wash, #fde8e8)",
      badgeText: "var(--error, #c64545)",
    };
  }
  if (c.includes("restor") || c.includes("filling") || c.includes("amalgam") || c.includes("composite")) {
    return {
      key: "restored",
      label: "Restored",
      badgeBg: "var(--info-wash, #e6f6f4)",
      badgeText: "var(--accent-teal, #5db8a6)",
    };
  }
  if (c.includes("crown") || c.includes("cap")) {
    return {
      key: "crown",
      label: "Crown",
      badgeBg: "var(--surface-cream-strong, #fef3c7)",
      badgeText: "#92400e",
    };
  }
  if (c.includes("implant") || c.includes("screw") || c.includes("fixture")) {
    return {
      key: "implant",
      label: "Implant",
      badgeBg: "var(--success-wash, #eaf6ed)",
      badgeText: "var(--success, #5db872)",
    };
  }

  // Deterministic adaptive color for custom conditions
  const customPalette = [
    { color: "#8b5cf6", bg: "#f5f3ff", text: "#6d28d9" },
    { color: "#cc785c", bg: "#fbf3ef", text: "#9c4224" },
    { color: "#06b6d4", bg: "#ecfeff", text: "#0e7490" },
    { color: "#ec4899", bg: "#fdf2f8", text: "#be185d" },
    { color: "#eab308", bg: "#fefce8", text: "#a16207" },
  ];
  let hash = 0;
  for (let i = 0; i < c.length; i++) {
    hash = (hash << 5) - hash + c.charCodeAt(i);
    hash |= 0;
  }
  const picked = customPalette[Math.abs(hash) % customPalette.length]!;

  return {
    key: "custom",
    label: condition,
    badgeBg: picked.bg,
    badgeText: picked.text,
    customColor: picked.color,
  };
}

export function VisualTooth({
  toothNum,
  fdi,
  name,
  condition,
  isSelected,
  notes,
  size = "md",
  onClick,
}: VisualToothProps) {
  const reactId = useId().replace(/:/g, "_");
  const anatomy = getToothAnatomyType(toothNum);
  const isMaxillary = toothNum >= 1 && toothNum <= 16;
  const isMandibular = toothNum >= 17 && toothNum <= 32;
  const normalized = normalizeCondition(condition);

  const hasNotes = Boolean(notes && notes.trim().length > 0);

  // Unique IDs for SVG gradients and filters to prevent collision across 32 teeth
  const enamelGradId = `enamel-grad-${toothNum}-${reactId}`;
  const rootGradId = `root-grad-${toothNum}-${reactId}`;
  const implantGradId = `implant-grad-${toothNum}-${reactId}`;
  const crownGradId = `crown-grad-${toothNum}-${reactId}`;
  const restoredGradId = `restored-grad-${toothNum}-${reactId}`;
  const cariesGradId = `caries-grad-${toothNum}-${reactId}`;
  const customGlowId = `custom-glow-${toothNum}-${reactId}`;

  // SVG coordinate box: 0 0 64 90
  // In canonical coordinates:
  // Roots: y: 5 to 46 (pointing UP towards maxilla)
  // Crown: y: 46 to 85 (pointing DOWN towards bite line)
  // For mandibular teeth (17-32), the group is vertically inverted around y=45:
  // transform="translate(0, 90) scale(1, -1)"
  // So crown points UP towards the central bite line, and roots point DOWN into the mandible!
  const groupTransform = isMandibular ? "translate(0, 90) scale(1, -1)" : undefined;

  return (
    <button
      type="button"
      className={`visual-tooth-card size-${size} condition-${normalized.key} ${isSelected ? "is-selected" : ""}`}
      onClick={onClick}
      aria-pressed={isSelected}
      aria-label={`Tooth #${toothNum} (FDI ${fdi}) - ${name}. Condition: ${normalized.label}${hasNotes ? `. Notes: ${notes}` : ""}`}
      title={`#${toothNum} (FDI ${fdi}) - ${name}\nCondition: ${normalized.label}${hasNotes ? `\nNotes: ${notes}` : ""}`}
      style={
        normalized.key === "custom" && isSelected && normalized.customColor
          ? {
              boxShadow: `0 0 0 3px ${normalized.customColor}44, 0 8px 20px -2px ${normalized.customColor}33`,
              borderColor: normalized.customColor,
            }
          : undefined
      }
    >
      {/* Top Header: FDI & Universal Tooth Number Badge */}
      <div className="visual-tooth-header">
        <span className="visual-tooth-fdi">{fdi}</span>
        <span className="visual-tooth-num">#{toothNum}</span>
      </div>

      {/* SVG Anatomical Tooth Visualization */}
      <svg
        className="visual-tooth-svg"
        viewBox="0 0 64 90"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <defs>
          {/* Sound Enamel Gradient (Pearlescent Ivory #fdfcf9 to #efe8dd) */}
          <linearGradient id={enamelGradId} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#ffffff" />
            <stop offset="25%" stopColor="#fdfcf9" />
            <stop offset="70%" stopColor="#f8f4ec" />
            <stop offset="100%" stopColor="#efe8dd" />
          </linearGradient>

          {/* Root Cementum Gradient */}
          <linearGradient id={rootGradId} x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#ece4d8" />
            <stop offset="50%" stopColor="#dfd2be" />
            <stop offset="100%" stopColor="#cfbeaa" />
          </linearGradient>

          {/* Titanium Implant Gradient (Threaded post metallic sheen) */}
          <linearGradient id={implantGradId} x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#475569" />
            <stop offset="20%" stopColor="#94a3b8" />
            <stop offset="45%" stopColor="#f8fafc" />
            <stop offset="75%" stopColor="#94a3b8" />
            <stop offset="100%" stopColor="#334155" />
          </linearGradient>

          {/* Polished Gold / Ceramic Crown Gradient */}
          <linearGradient id={crownGradId} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#fef08a" />
            <stop offset="25%" stopColor="#f59e0b" />
            <stop offset="50%" stopColor="#fbbf24" />
            <stop offset="80%" stopColor="#d97706" />
            <stop offset="100%" stopColor="#92400e" />
          </linearGradient>

          {/* Restored Composite / Amalgam Inlay Gradient */}
          <linearGradient id={restoredGradId} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#7ee2d0" />
            <stop offset="45%" stopColor="#5db8a6" />
            <stop offset="100%" stopColor="#2e7568" />
          </linearGradient>

          {/* Caries Cavitation Cavity Radial Pit Gradient */}
          <radialGradient id={cariesGradId} cx="45%" cy="45%" r="55%">
            <stop offset="0%" stopColor="#0d0502" />
            <stop offset="50%" stopColor="#2e1408" />
            <stop offset="85%" stopColor="#5a290f" />
            <stop offset="100%" stopColor="#7c3a17" stopOpacity="0.8" />
          </radialGradient>

          {/* Subtle Glow Filter for Custom Condition */}
          {normalized.key === "custom" && normalized.customColor && (
            <filter id={customGlowId} x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow
                dx="0"
                dy="0"
                stdDeviation="2"
                floodColor={normalized.customColor}
                floodOpacity="0.6"
              />
            </filter>
          )}
        </defs>

        <g transform={groupTransform}>
          {/* ========================================================= */}
          {/* ROOT LAYER: Either Titanium Implant Screw or Natural Roots */}
          {/* ========================================================= */}
          {normalized.key === "implant" ? (
            /* Metallic Titanium Threaded Screw Post & Abutment Collar */
            <g className="visual-tooth-implant">
              {/* Abutment Collar at the Cervical Neck */}
              <path
                d="M 22 46 L 24 39 L 40 39 L 42 46 Z"
                fill={`url(#${implantGradId})`}
                stroke="#334155"
                strokeWidth="1"
              />
              {/* Screw Post Tapered Body */}
              <path
                d="M 24 39 L 27 9 C 28 6 36 6 37 9 L 40 39 Z"
                fill={`url(#${implantGradId})`}
                stroke="#334155"
                strokeWidth="1"
              />
              {/* Helical Thread Notches & Ridges */}
              <line x1="23.5" y1="35" x2="40.5" y2="35" stroke="#1e293b" strokeWidth="1.6" strokeLinecap="round" />
              <line x1="24.2" y1="30" x2="39.8" y2="30" stroke="#1e293b" strokeWidth="1.6" strokeLinecap="round" />
              <line x1="24.9" y1="25" x2="39.1" y2="25" stroke="#1e293b" strokeWidth="1.6" strokeLinecap="round" />
              <line x1="25.6" y1="20" x2="38.4" y2="20" stroke="#1e293b" strokeWidth="1.6" strokeLinecap="round" />
              <line x1="26.3" y1="15" x2="37.7" y2="15" stroke="#1e293b" strokeWidth="1.6" strokeLinecap="round" />
              <line x1="27.0" y1="10" x2="37.0" y2="10" stroke="#1e293b" strokeWidth="1.6" strokeLinecap="round" />

              {/* Specular Titanium Reflection Highlight */}
              <line x1="32" y1="8" x2="32" y2="39" stroke="#ffffff" strokeWidth="1.4" strokeOpacity="0.85" />
            </g>
          ) : (
            /* Natural Biological Root Anatomies */
            <g className="visual-tooth-root">
              {anatomy === "molar" && (
                isMaxillary ? (
                  /* Maxillary Molar (3 Roots: Mesiobuccal, Distobuccal, Palatal) */
                  <g>
                    <path
                      d="M 15 46 C 13 36 9 24 9 13 C 9 6 17 6 18 12 C 20 23 23 35 25 38 C 27 28 29 16 31 6 C 32 4 34 4 35 6 C 37 16 39 28 41 38 C 43 35 46 23 48 12 C 49 6 57 6 57 13 C 57 24 53 36 50 46 C 38 48 26 48 15 46 Z"
                      fill={`url(#${rootGradId})`}
                      stroke="#bead98"
                      strokeWidth="1"
                    />
                    {/* Root Canal Lines / Shading */}
                    <path d="M 14 42 Q 13 25 13 13" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                    <path d="M 33 42 L 33 9" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                    <path d="M 51 42 Q 52 25 52 13" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                  </g>
                ) : (
                  /* Mandibular Molar (2 Roots: Mesial & Distal with Deep Furcation) */
                  <g>
                    <path
                      d="M 13 46 C 11 36 10 22 11 12 C 12 6 20 6 22 11 C 24 22 26 33 32 37 C 38 33 40 22 42 11 C 44 6 52 6 53 12 C 54 22 53 36 51 46 C 40 48 24 48 13 46 Z"
                      fill={`url(#${rootGradId})`}
                      stroke="#bead98"
                      strokeWidth="1"
                    />
                    {/* Root Canal Lines */}
                    <path d="M 16 42 Q 15 25 16 11" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                    <path d="M 48 42 Q 49 25 48 11" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                  </g>
                )
              )}

              {anatomy === "premolar" && (
                /* Premolar Tapered Root */
                <g>
                  <path
                    d="M 21 46 C 22 34 26 18 31 7 C 32 5 34 5 35 7 C 38 18 42 34 43 46 C 36 47 28 47 21 46 Z"
                    fill={`url(#${rootGradId})`}
                    stroke="#bead98"
                    strokeWidth="1"
                  />
                  <path d="M 32.5 42 L 32.5 11" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                </g>
              )}

              {anatomy === "canine" && (
                /* Canine Massive Single Anchor Root */
                <g>
                  <path
                    d="M 19 46 C 20 33 26 15 31 4 C 32 3 33 3 34 4 C 38 15 44 33 45 46 C 37 47 27 47 19 46 Z"
                    fill={`url(#${rootGradId})`}
                    stroke="#bead98"
                    strokeWidth="1"
                  />
                  <path d="M 32 42 L 32 8" stroke="#b4a38d" strokeWidth="1.4" strokeLinecap="round" />
                </g>
              )}

              {anatomy === "incisor" && (
                /* Incisor Straight Tapered Root */
                <g>
                  <path
                    d="M 21 46 C 23 35 27 18 31 7 C 32 6 33 6 34 7 C 38 18 42 35 43 46 C 36 47 28 47 21 46 Z"
                    fill={`url(#${rootGradId})`}
                    stroke="#bead98"
                    strokeWidth="1"
                  />
                  <path d="M 32 42 L 32 10" stroke="#b4a38d" strokeWidth="1.2" strokeLinecap="round" />
                </g>
              )}
            </g>
          )}

          {/* Cervical Margin Junction Line (CEJ) */}
          <path
            d="M 16 46 C 26 48 38 48 48 46"
            stroke="#cfbeaa"
            strokeWidth="1.2"
            fill="none"
            strokeLinecap="round"
          />

          {/* ========================================================= */}
          {/* CROWN LAYER: Anatomical Contour, Fissures, & Conditions   */}
          {/* ========================================================= */}
          <g className="visual-tooth-crown">
            {/* 1. Molar Crown (Wide Occlusal Table, Multi-Cusp Contours) */}
            {anatomy === "molar" && (
              <g>
                <path
                  d="M 12 46 C 8 53 8 72 13 80 C 18 86 26 84 32 82 C 38 84 46 86 51 80 C 56 72 56 53 52 46 C 42 48 22 48 12 46 Z"
                  fill={normalized.key === "crown" ? `url(#${crownGradId})` : `url(#${enamelGradId})`}
                  stroke={
                    normalized.key === "custom" && normalized.customColor
                      ? normalized.customColor
                      : normalized.key === "crown"
                      ? "#92400e"
                      : "#d4c8b8"
                  }
                  strokeWidth="1.2"
                  filter={normalized.key === "custom" && normalized.customColor ? `url(#${customGlowId})` : undefined}
                />

                {/* Natural Occlusal Grooves / Fissures */}
                {normalized.key !== "crown" && (
                  <g opacity={normalized.key === "caries" ? 0.4 : 0.85}>
                    {/* Central Groove */}
                    <path d="M 21 66 Q 32 68 43 66" stroke="#c4b5a2" strokeWidth="1.3" strokeLinecap="round" fill="none" />
                    {/* Buccal/Lingual Grooves */}
                    <path d="M 32 54 L 32 78" stroke="#c4b5a2" strokeWidth="1.3" strokeLinecap="round" fill="none" />
                    {/* Cusp Fissure Branches */}
                    <path d="M 25 66 Q 19 59 16 57" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" fill="none" />
                    <path d="M 25 66 Q 19 73 16 75" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" fill="none" />
                    <path d="M 39 66 Q 45 59 48 57" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" fill="none" />
                    <path d="M 39 66 Q 45 73 48 75" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" fill="none" />
                  </g>
                )}
              </g>
            )}

            {/* 2. Premolar Crown (Dual-Cusp Anatomy, Bicuspid Table) */}
            {anatomy === "premolar" && (
              <g>
                <path
                  d="M 16 46 C 12 54 13 73 18 80 C 23 85 28 83 32 81 C 36 83 41 85 46 80 C 51 73 52 54 48 46 C 40 47 24 47 16 46 Z"
                  fill={normalized.key === "crown" ? `url(#${crownGradId})` : `url(#${enamelGradId})`}
                  stroke={
                    normalized.key === "custom" && normalized.customColor
                      ? normalized.customColor
                      : normalized.key === "crown"
                      ? "#92400e"
                      : "#d4c8b8"
                  }
                  strokeWidth="1.2"
                  filter={normalized.key === "custom" && normalized.customColor ? `url(#${customGlowId})` : undefined}
                />

                {/* Dual Cusp Developmental Groove */}
                {normalized.key !== "crown" && (
                  <g opacity={normalized.key === "caries" ? 0.4 : 0.85}>
                    <path d="M 22 66 Q 32 67 42 66" stroke="#c4b5a2" strokeWidth="1.3" strokeLinecap="round" fill="none" />
                    <path d="M 25 66 L 20 62" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" />
                    <path d="M 25 66 L 20 70" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" />
                    <path d="M 39 66 L 44 62" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" />
                    <path d="M 39 66 L 44 70" stroke="#c4b5a2" strokeWidth="1.1" strokeLinecap="round" />
                  </g>
                )}
              </g>
            )}

            {/* 3. Canine Crown (Prominent Pointed Cusp Tip & Diamond Edge) */}
            {anatomy === "canine" && (
              <g>
                <path
                  d="M 18 46 C 14 54 14 69 19 76 L 32 86 L 45 76 C 50 69 50 54 46 46 C 38 47 26 47 18 46 Z"
                  fill={normalized.key === "crown" ? `url(#${crownGradId})` : `url(#${enamelGradId})`}
                  stroke={
                    normalized.key === "custom" && normalized.customColor
                      ? normalized.customColor
                      : normalized.key === "crown"
                      ? "#92400e"
                      : "#d4c8b8"
                  }
                  strokeWidth="1.2"
                  filter={normalized.key === "custom" && normalized.customColor ? `url(#${customGlowId})` : undefined}
                />

                {/* Prominent Labial Ridge */}
                {normalized.key !== "crown" && (
                  <g opacity={normalized.key === "caries" ? 0.4 : 0.85}>
                    <path d="M 32 48 L 32 82" stroke="#ffffff" strokeWidth="1.2" strokeLinecap="round" />
                    <path d="M 23 74 L 32 83 L 41 74" stroke="#c4b5a2" strokeWidth="1" strokeLinecap="round" fill="none" />
                  </g>
                )}
              </g>
            )}

            {/* 4. Incisor Crown (Chisel / Shovel Shaped Incisal Edge) */}
            {anatomy === "incisor" && (
              <g>
                <path
                  d="M 17 46 C 14 54 13 70 14 80 C 15 82 17 83 20 83 L 44 83 C 47 83 49 82 50 80 C 51 70 50 54 47 46 C 39 47 25 47 17 46 Z"
                  fill={normalized.key === "crown" ? `url(#${crownGradId})` : `url(#${enamelGradId})`}
                  stroke={
                    normalized.key === "custom" && normalized.customColor
                      ? normalized.customColor
                      : normalized.key === "crown"
                      ? "#92400e"
                      : "#d4c8b8"
                  }
                  strokeWidth="1.2"
                  filter={normalized.key === "custom" && normalized.customColor ? `url(#${customGlowId})` : undefined}
                />

                {/* Shovel Margin & Developmental Depression Lobes */}
                {normalized.key !== "crown" && (
                  <g opacity={normalized.key === "caries" ? 0.4 : 0.85}>
                    <path d="M 18 80 L 46 80" stroke="#c4b5a2" strokeWidth="1" strokeLinecap="round" />
                    <path d="M 26 56 L 26 76" stroke="#ffffff" strokeWidth="1" strokeLinecap="round" />
                    <path d="M 38 56 L 38 76" stroke="#ffffff" strokeWidth="1" strokeLinecap="round" />
                  </g>
                )}
              </g>
            )}

            {/* ========================================================= */}
            {/* SPECIAL CLINICAL CONDITION OVERLAYS                       */}
            {/* ========================================================= */}

            {/* A. CARIES: Cavitation Pit & Radiating Decay Fissures */}
            {normalized.key === "caries" && (
              <g className="visual-tooth-condition-caries">
                {/* Necrotic Halo */}
                <ellipse cx="32" cy="66" rx="9" ry="7" fill="#78350f" fillOpacity="0.22" />

                {/* Dark Cavitation Pit */}
                <path
                  d="M 27 63 C 24 61 23 64 21 66 C 20 69 22 72 25 72 C 29 73 31 70 34 71 C 38 72 41 69 42 66 C 43 63 40 61 37 62 C 34 60 30 62 27 63 Z"
                  fill={`url(#${cariesGradId})`}
                  stroke="#1a0c04"
                  strokeWidth="0.8"
                />

                {/* Jagged Decay Fissure Micro-Cracks */}
                <path d="M 21 66 Q 16 65 13 62" stroke="#1f1108" strokeWidth="1.5" strokeLinecap="round" fill="none" />
                <path d="M 42 66 Q 47 67 50 69" stroke="#1f1108" strokeWidth="1.5" strokeLinecap="round" fill="none" />
                <path d="M 27 63 Q 28 56 30 53" stroke="#1f1108" strokeWidth="1.5" strokeLinecap="round" fill="none" />
                <path d="M 34 71 Q 35 77 33 80" stroke="#1f1108" strokeWidth="1.5" strokeLinecap="round" fill="none" />
              </g>
            )}

            {/* B. RESTORED: Composite / Amalgam Inlay Filling */}
            {normalized.key === "restored" && (
              <g className="visual-tooth-condition-restored">
                <path
                  d="M 23 62 C 21 63 20 66 22 68 C 24 70 28 69 31 70 C 35 71 38 69 40 69 C 43 68 44 65 42 63 C 40 61 36 63 32 62 C 28 61 25 61 23 62 Z"
                  fill={`url(#${restoredGradId})`}
                  stroke="#134e48"
                  strokeWidth="1.1"
                />
                {/* Etched Inlay Preparation Margin */}
                <path
                  d="M 22 68 Q 32 72 40 69"
                  stroke="#0f766e"
                  strokeWidth="0.8"
                  fill="none"
                />
                {/* Restorative Polish Specular Luster */}
                <ellipse cx="31" cy="65" rx="3.5" ry="1.8" fill="#a7f3d0" fillOpacity="0.85" />
              </g>
            )}

            {/* C. CROWN: Gold / Ceramic Polished Cap Collar & Highlight Sheen */}
            {normalized.key === "crown" && (
              <g className="visual-tooth-condition-crown">
                {/* Cervical Crown Collar Margin Ring */}
                <path
                  d="M 14 46 C 24 49 40 49 50 46"
                  stroke="#78350f"
                  strokeWidth="2.4"
                  fill="none"
                  strokeLinecap="round"
                />
                <path
                  d="M 16 46.5 C 26 49 38 49 48 46.5"
                  stroke="#fef08a"
                  strokeWidth="1"
                  fill="none"
                  strokeLinecap="round"
                />

                {/* Polished High-Gloss Diagonal Specular Sheen */}
                <path
                  d="M 21 53 Q 28 64 36 78"
                  stroke="#ffffff"
                  strokeWidth="3.2"
                  strokeLinecap="round"
                  strokeOpacity="0.45"
                  fill="none"
                />
              </g>
            )}

            {/* Enamel Translucent Contour Reflection (Sound & Natural teeth) */}
            {normalized.key === "sound" && (
              <path
                d="M 20 52 C 24 50 40 50 44 52"
                stroke="#ffffff"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeOpacity="0.7"
                fill="none"
              />
            )}
          </g>
        </g>
      </svg>

      {/* Bottom Status Tag: Presets or Adaptive Custom Condition Badge */}
      <span
        className="visual-tooth-tag"
        style={{
          backgroundColor: normalized.badgeBg,
          color: normalized.badgeText,
          borderColor: normalized.key === "custom" ? normalized.badgeText : undefined,
        }}
      >
        {normalized.label}
      </span>

      {/* Notes Indicator */}
      {hasNotes && (
        <div className="visual-tooth-notes" title={notes}>
          <i className="ph ph-note-pencil" aria-hidden="true" style={{ flexShrink: 0 }} />
          <span>{notes}</span>
        </div>
      )}
    </button>
  );
}
