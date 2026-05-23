export interface KbDocumentFile {
  path: string;
  title: string;
  fileType: string;
  size: number;
  lastModified: number;
  chunkCount: number;
}

export interface KbIngestResult {
  knowledgeBasePath: string;
  fileCount: number;
  chunkCount: number;
  message: string;
}

export interface KbSearchResult {
  id: string;
  content: string;
  score?: number;
  metadata: Record<string, unknown>;
}
