<template>
  <div class="workflow-editor">
    <div class="editor-header">
      <h2>{{ isEdit ? '编辑工作流' : '创建工作流' }}</h2>
      <div class="actions">
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="saveWorkflow">保存</el-button>
      </div>
    </div>

    <div class="editor-container">
      <el-form :model="form" label-width="100px" class="workflow-form">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="请输入工作流名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入工作流描述"
          />
        </el-form-item>
      </el-form>

      <el-card class="designer-card">
        <template #header>
          <div class="card-header">
            <span>工作流设计器</span>
            <el-button link type="primary" @click="showHelp">
              <el-icon><QuestionFilled /></el-icon>
              帮助
            </el-button>
          </div>
        </template>
        <div class="designer-placeholder">
          <el-empty description="工作流设计器将在后续版本实现">
            <el-button type="primary" @click="createSimpleWorkflow">创建简单示例</el-button>
          </el-empty>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)

const form = ref({
  name: '',
  description: '',
  definition: {
    nodes: [],
    edges: [],
  },
})

const cancel = () => {
  router.back()
}

const saveWorkflow = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入工作流名称')
    return
  }
  // TODO: 调用后端API
  ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
  router.push('/workflows')
}

const showHelp = () => {
  ElMessage.info('工作流设计器使用指南')
}

const createSimpleWorkflow = () => {
  form.value.name = '示例工作流'
  form.value.description = '这是一个示例工作流'
  ElMessage.success('已创建示例配置')
}
</script>

<style scoped>
.workflow-editor {
  padding: 20px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.editor-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.workflow-form {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.designer-card {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.designer-placeholder {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
