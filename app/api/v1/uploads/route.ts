// File uploads for report attachments.
//
// Known limitation: this stores uploaded files on local/server disk under
// public/uploads/reports/. That works for local dev and for a single-instance
// / self-hosted deployment, but it is NOT durable on ephemeral or serverless
// hosts (e.g. Vercel's production filesystem is read-only, and even where a
// write succeeds on a serverless instance it disappears once that instance
// recycles). Swapping this for real object storage (S3, Vercel Blob, etc.) is
// a known follow-up and intentionally out of scope here.

import { NextResponse } from "next/server";
import { newId } from "@/lib/db";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

const MAX_UPLOAD_BYTES = 25 * 1024 * 1024; // 25MB

/** Strips path separators and anything outside [A-Za-z0-9._-], then collapses repeats. */
function sanitizeFilename(name: string): string {
  const withoutPathSeparators = name.replace(/[\\/]/g, "_");
  const safeChars = withoutPathSeparators.replace(/[^A-Za-z0-9._-]/g, "_");
  const collapsed = safeChars.replace(/_+/g, "_");
  return collapsed || "file";
}

export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const file = formData.get("file");

    if (!(file instanceof File) || file.size === 0) {
      return NextResponse.json(
        { status: "error", message: "No file provided under field 'file'." },
        { status: 400 }
      );
    }

    if (file.size > MAX_UPLOAD_BYTES) {
      return NextResponse.json(
        { status: "error", message: "File exceeds the 25MB upload limit." },
        { status: 400 }
      );
    }

    const sanitizedName = sanitizeFilename(file.name || "file");
    const uniqueId = newId("upl");
    const finalName = `${uniqueId}-${sanitizedName}`;

    const uploadDir = path.join(process.cwd(), "public", "uploads", "reports");
    await mkdir(uploadDir, { recursive: true });

    const bytes = Buffer.from(await file.arrayBuffer());
    await writeFile(path.join(uploadDir, finalName), bytes);

    return NextResponse.json(
      {
        status: "success",
        data: {
          url: `/uploads/reports/${finalName}`,
          filename: file.name,
          fileType: file.type || null,
        },
      },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to upload file";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
