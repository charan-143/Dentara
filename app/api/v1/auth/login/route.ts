import { NextResponse } from "next/server";
import { signIn, findAccountByEmail, createApiSession } from "@/lib/auth";
import { db } from "@/lib/db";

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 200,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    },
  });
}

export async function POST(request: Request) {
  try {
    const body = await request.json().catch(() => ({}));
    const email = body.email ? String(body.email).trim().toLowerCase() : "";
    const password = body.password ? String(body.password) : "";

    if (!email || !password) {
      return NextResponse.json(
        { status: "error", message: "Email and password are required." },
        { status: 400 }
      );
    }

    // Verify credentials against database
    const signInResult = await signIn(email, password);
    if (!signInResult.ok) {
      return NextResponse.json(
        { status: "error", message: signInResult.error || "Invalid credentials" },
        { status: 401 }
      );
    }

    const account = await findAccountByEmail(email);
    if (!account) {
      return NextResponse.json(
        { status: "error", message: "Account not found." },
        { status: 401 }
      );
    }

    const sql = db();
    const clinicians = (await sql.query(
      "SELECT id, name, credentials, room FROM clinicians WHERE id = $1",
      [account.clinician_id]
    )) as Array<Record<string, any>>;
    const c = clinicians[0];

    const profile = {
      id: c?.id || account.clinician_id,
      name: c?.name || "Clinician",
      role: account.role || "clinician",
      credentials: c?.credentials || "BDS, MSc",
      room: c?.room || "Surgery 1",
    };

    const userAgent = request.headers.get("user-agent") || "";
    const session = await createApiSession(account, userAgent);

    return NextResponse.json(
      {
        status: "success",
        token: session.token,
        session_token: session.token,
        profile,
        user: profile,
      },
      { status: 200 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Internal server error during authentication";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
