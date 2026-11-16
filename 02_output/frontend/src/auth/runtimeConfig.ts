import { z } from "zod";

/** /config.json, written by the frontend container at start (ADR-05, runtime-config.schema.json). */
const runtimeConfigSchema = z
  .object({
    firebaseApiKey: z.string().min(1),
    firebaseAuthDomain: z.string().min(1),
    firebaseProjectId: z.string().min(1),
    authEmulatorUrl: z
      .string()
      .regex(/^https?:\/\//)
      .optional(),
  })
  .strict();

export type RuntimeConfig = z.infer<typeof runtimeConfigSchema>;

/** Loads and checks the settings of this environment. */
export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const response = await fetch("/config.json", { cache: "no-store" });
  if (!response.ok) throw new Error(`/config.json answered ${response.status}`);
  return runtimeConfigSchema.parse(await response.json());
}
