import type { KbDocumentFile, KbIngestResult, KbSearchResult } from "@/types/knowledge";

async function requestJson<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`后端返回 ${response.status} ${response.statusText}`);
  }
  return (await response.json()) as T;
}

export function listKnowledgeFiles() {
  return requestJson<KbDocumentFile[]>("/api/admin/kb/theme-business/files");
}

export function ingestAllKnowledge() {
  return requestJson<KbIngestResult>("/api/admin/kb/ingest/theme-business", {
    method: "POST"
  });
}

export function ingestKnowledgeFile(path: string) {
  const params = new URLSearchParams({ path });
  return requestJson<KbIngestResult>(`/api/admin/kb/ingest/theme-business/file?${params}`, {
    method: "POST"
  });
}

export function deleteKnowledgeFileVectors(path: string) {
  const params = new URLSearchParams({ path });
  return requestJson<KbIngestResult>(`/api/admin/kb/delete/theme-business/file?${params}`, {
    method: "POST"
  });
}

export function searchKnowledge(query: string) {
  const params = new URLSearchParams({ query });
  return requestJson<KbSearchResult[]>(`/api/admin/kb/search?${params}`);
}
