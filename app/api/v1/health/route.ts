import { NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function GET() {
  try {
    const sql = db();
    await sql.query("SELECT 1");

    return NextResponse.json({
      status: "ok",
      database: "connected",
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Database health check failed";
    return NextResponse.json(
      {
        status: "error",
        database: "disconnected",
        message,
        timestamp: new Date().toISOString(),
      },
      { status: 500 }
    );
  }
}
