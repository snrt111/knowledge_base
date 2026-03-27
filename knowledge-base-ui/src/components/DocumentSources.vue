<template>
  <div class="document-sources">
    <div class="sources-header" @click="toggle">
      <div class="sources-header-left">
        <el-icon><Document /></el-icon>
        <span>参考来源 ({{ sources.length }}个)</span>
      </div>
      <el-icon class="sources-toggle-icon" :class="{ 'is-expanded': !isCollapsed }">
        <ArrowDown />
      </el-icon>
    </div>
    <div v-show="!isCollapsed" class="sources-list">
      <div
        v-for="(source, index) in sources"
        :key="index"
        class="source-item"
      >
        <div class="source-main" @click="handleViewDocument(source.documentId)">
          <div class="source-info">
            <div class="source-title-row">
              <span class="source-name">{{ source.documentName }}</span>
              <span v-if="hasScore(source.score)" class="source-score">
                匹配度: {{ formatScore(source.score) }}
              </span>
            </div>
            <span class="source-kb">{{ source.knowledgeBaseName }}</span>
          </div>
          <el-icon class="source-arrow"><ArrowRight /></el-icon>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Document, ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import type { DocumentSource } from '@/types'

interface Props {
  sources: DocumentSource[]
  defaultCollapsed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  defaultCollapsed: true
})

const emit = defineEmits<{
  viewDocument: [documentId: string]
}>()

const isCollapsed = ref(props.defaultCollapsed)

const toggle = () => {
  isCollapsed.value = !isCollapsed.value
}

const hasScore = (score?: number): boolean => {
  return score !== null && score !== undefined
}

const formatScore = (score?: number): string => {
  if (score === undefined || score === null) return '0%'
  const percentage = Math.max(0, Math.min(100, score * 100))
  return `${percentage.toFixed(1)}%`
}

const handleViewDocument = (documentId: string) => {
  emit('viewDocument', documentId)
}
</script>

<style scoped>
.document-sources {
  margin-top: 12px;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.sources-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
  cursor: pointer;
  padding: 4px 0;
  user-select: none;
}

.sources-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sources-toggle-icon {
  font-size: 14px;
  color: #909399;
  transition: transform 0.3s ease;
}

.sources-toggle-icon.is-expanded {
  transform: rotate(180deg);
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.source-item {
  display: flex;
  flex-direction: column;
  padding: 8px 12px;
  background-color: #fff;
  border-radius: 6px;
  transition: all 0.2s;
  border: 1px solid #e4e7ed;
}

.source-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.source-item:hover {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.source-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.source-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.source-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.source-score {
  font-size: 11px;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  font-weight: 500;
}

.source-kb {
  font-size: 11px;
  color: #909399;
}

.source-arrow {
  color: #c0c4cc;
  font-size: 14px;
  margin-left: 8px;
}

.source-main:hover .source-arrow {
  color: #409eff;
}
</style>
