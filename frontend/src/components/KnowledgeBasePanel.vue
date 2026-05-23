<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import {
  deleteKnowledgeFileVectors,
  ingestAllKnowledge,
  ingestKnowledgeFile,
  listKnowledgeFiles,
  searchKnowledge
} from "@/services/knowledgeBaseService";
import type { KbDocumentFile, KbIngestResult, KbSearchResult } from "@/types/knowledge";

const files = ref<KbDocumentFile[]>([]);
const searchResults = ref<KbSearchResult[]>([]);
const selectedPath = ref("");
const searchQuery = ref("主题审核通过了为什么前台看不到");
const loading = ref(false);
const error = ref("");
const result = ref<KbIngestResult | null>(null);

const selectedFile = computed(() => files.value.find((item) => item.path === selectedPath.value));

onMounted(() => {
  refreshFiles();
});

async function runAction(action: () => Promise<KbIngestResult | KbDocumentFile[] | KbSearchResult[]>) {
  loading.value = true;
  error.value = "";
  try {
    return await action();
  } catch (err) {
    error.value = err instanceof Error ? err.message : "操作失败";
    return null;
  } finally {
    loading.value = false;
  }
}

async function refreshFiles() {
  const data = await runAction(listKnowledgeFiles);
  if (Array.isArray(data)) {
    files.value = data as KbDocumentFile[];
    if (!selectedPath.value && files.value.length > 0) {
      selectedPath.value = files.value[0].path;
    }
  }
}

async function ingestAll() {
  const data = await runAction(ingestAllKnowledge);
  if (data && !Array.isArray(data)) {
    result.value = data as KbIngestResult;
  }
}

async function ingestSelected() {
  if (!selectedPath.value) {
    return;
  }
  const data = await runAction(() => ingestKnowledgeFile(selectedPath.value));
  if (data && !Array.isArray(data)) {
    result.value = data as KbIngestResult;
  }
}

async function deleteSelected() {
  if (!selectedPath.value) {
    return;
  }
  const data = await runAction(() => deleteKnowledgeFileVectors(selectedPath.value));
  if (data && !Array.isArray(data)) {
    result.value = data as KbIngestResult;
  }
}

async function search() {
  if (!searchQuery.value.trim()) {
    return;
  }
  const data = await runAction(() => searchKnowledge(searchQuery.value.trim()));
  if (Array.isArray(data)) {
    searchResults.value = data as KbSearchResult[];
  }
}

function formatSize(value: number) {
  if (value < 1024) {
    return `${value} B`;
  }
  return `${(value / 1024).toFixed(1)} KB`;
}

function formatDate(value: number) {
  return new Date(value).toLocaleString();
}
</script>

<template>
  <section class="kb-layout">
    <div class="kb-toolbar">
      <div>
        <h2>知识库管理</h2>
        <p>按文件导入 Redis Stack 向量索引，避免每次全量重建。</p>
      </div>
      <div class="kb-actions">
        <button class="ghost-btn" type="button" :disabled="loading" @click="refreshFiles">刷新文件</button>
        <button class="send-btn" type="button" :disabled="loading" @click="ingestAll">全量导入</button>
      </div>
    </div>

    <div v-if="error" class="error-box">{{ error }}</div>

    <div class="kb-grid">
      <section class="panel kb-file-list">
        <div class="panel-title">知识文件</div>
        <button
          v-for="file in files"
          :key="file.path"
          class="kb-file"
          :class="{ active: file.path === selectedPath }"
          type="button"
          @click="selectedPath = file.path"
        >
          <strong>{{ file.title }}</strong>
          <span>{{ file.path }}</span>
          <small>{{ file.fileType }} · {{ file.chunkCount }} chunks · {{ formatSize(file.size) }}</small>
        </button>
      </section>

      <section class="kb-detail">
        <div class="panel">
          <div class="panel-title">文件操作</div>
          <div v-if="selectedFile" class="kb-card">
            <h3>{{ selectedFile.title }}</h3>
            <dl class="meta">
              <div><dt>路径</dt><dd>{{ selectedFile.path }}</dd></div>
              <div><dt>类型</dt><dd>{{ selectedFile.fileType }}</dd></div>
              <div><dt>切片</dt><dd>{{ selectedFile.chunkCount }}</dd></div>
              <div><dt>大小</dt><dd>{{ formatSize(selectedFile.size) }}</dd></div>
              <div><dt>修改时间</dt><dd>{{ formatDate(selectedFile.lastModified) }}</dd></div>
            </dl>
            <div class="kb-actions inline">
              <button class="send-btn" type="button" :disabled="loading" @click="ingestSelected">导入/重建当前文件</button>
              <button class="ghost-btn danger" type="button" :disabled="loading" @click="deleteSelected">删除当前文件向量</button>
            </div>
          </div>
        </div>

        <div v-if="result" class="panel">
          <div class="panel-title">最近结果</div>
          <dl class="meta">
            <div><dt>文件数</dt><dd>{{ result.fileCount }}</dd></div>
            <div><dt>切片数</dt><dd>{{ result.chunkCount }}</dd></div>
            <div><dt>目录</dt><dd>{{ result.knowledgeBasePath }}</dd></div>
            <div><dt>消息</dt><dd>{{ result.message }}</dd></div>
          </dl>
        </div>

        <div class="panel">
          <div class="panel-title">检索调试</div>
          <div class="kb-search">
            <input v-model="searchQuery" :disabled="loading" />
            <button class="send-btn" type="button" :disabled="loading" @click="search">检索</button>
          </div>
          <div class="kb-results">
            <article v-for="item in searchResults" :key="item.id" class="kb-result">
              <header>
                <strong>{{ item.metadata.title ?? "未知文档" }}</strong>
                <span>{{ item.score?.toFixed(4) ?? "--" }}</span>
              </header>
              <small>{{ item.metadata.sourcePath }}</small>
              <p>{{ item.content }}</p>
            </article>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>
