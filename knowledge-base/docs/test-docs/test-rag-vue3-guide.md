# Vue 3 完全指南

## 1. Vue 3 简介

Vue 3 是 Vue.js 的最新主要版本，于 2020 年 9 月正式发布。它在 Vue 2 的基础上进行了全面的改进，带来了更好的性能、更小的包体积和更强大的组合式 API。

### 1.1 Vue 3 的新特性

- **组合式 API (Composition API)**：更灵活的代码组织方式
- **性能提升**：渲染性能提升 1.3~2 倍，包体积减少 41%
- **TypeScript 支持**：更好的 TypeScript 类型推断
- **Teleport**：将组件渲染到 DOM 其他位置
- **Fragments**：组件可以有多个根节点
- **Suspense**：异步组件加载状态管理

### 1.2 创建 Vue 3 项目

使用 Vue CLI：

```bash
npm install -g @vue/cli
vue create my-project
# 选择 Vue 3
```

使用 Vite（推荐）：

```bash
npm create vite@latest my-project -- --template vue
npm create vite@latest my-project -- --template vue-ts
```

## 2. 组合式 API

### 2.1 setup 函数

`setup` 是组合式 API 的入口，在组件创建之前执行。

```vue
<script>
export default {
  setup() {
    // 组合式 API 代码
    const count = ref(0)
    const increment = () => count.value++

    return {
      count,
      increment
    }
  }
}
</script>
```

### 2.2 script setup 语法糖

Vue 3.2+ 推荐的写法：

```vue
<script setup>
import { ref, computed } from 'vue'

const count = ref(0)
const double = computed(() => count.value * 2)
const increment = () => count.value++
</script>
```

### 2.3 响应式基础

#### ref

```vue
<script setup>
import { ref } from 'vue'

// 基本类型
const count = ref(0)
console.log(count.value) // 0

// 对象
const user = ref({ name: 'John', age: 30 })
console.log(user.value.name) // John

// 修改值
count.value++
user.value.age = 31
</script>
```

#### reactive

```vue
<script setup>
import { reactive } from 'vue'

const state = reactive({
  count: 0,
  user: {
    name: 'John',
    age: 30
  }
})

// 直接访问属性
state.count++
state.user.age = 31
</script>
```

#### ref vs reactive

| 特性 | ref | reactive |
|------|-----|----------|
| 数据类型 | 任意类型 | 仅对象/数组 |
| 访问方式 | `.value` | 直接访问属性 |
| 解构/展开 | 保持响应式（需使用 toRefs） | 丢失响应式 |
| 适用场景 | 基本类型、需要替换的对象 | 复杂对象、表单数据 |

### 2.4 计算属性

```vue
<script setup>
import { ref, computed } from 'vue'

const firstName = ref('John')
const lastName = ref('Doe')

// 只读计算属性
const fullName = computed(() => {
  return `${firstName.value} ${lastName.value}`
})

// 可写计算属性
const fullNameWritable = computed({
  get: () => `${firstName.value} ${lastName.value}`,
  set: (newValue) => {
    [firstName.value, lastName.value] = newValue.split(' ')
  }
})
</script>
```

### 2.5 侦听器

```vue
<script setup>
import { ref, watch, watchEffect } from 'vue'

const count = ref(0)
const user = ref({ name: 'John', age: 30 })

// 监听单个 ref
watch(count, (newVal, oldVal) => {
  console.log(`count changed from ${oldVal} to ${newVal}`)
})

// 监听多个数据源
watch([count, () => user.value.name], ([newCount, newName], [oldCount, oldName]) => {
  console.log('Values changed')
})

// 深度监听对象
watch(user, (newVal) => {
  console.log('user changed:', newVal)
}, { deep: true })

// 立即执行监听器
watch(count, (newVal) => {
  console.log('Initial value:', newVal)
}, { immediate: true })

// watchEffect - 自动追踪依赖
watchEffect(() => {
  console.log(`Current count: ${count.value}`)
  // 自动追踪 count 的依赖
})
</script>
```

### 2.6 生命周期钩子

```vue
<script setup>
import {
  onBeforeMount,
  onMounted,
  onBeforeUpdate,
  onUpdated,
  onBeforeUnmount,
  onUnmounted
} from 'vue'

onBeforeMount(() => {
  console.log('组件挂载前')
})

onMounted(() => {
  console.log('组件已挂载')
})

onBeforeUpdate(() => {
  console.log('组件更新前')
})

onUpdated(() => {
  console.log('组件已更新')
})

onBeforeUnmount(() => {
  console.log('组件卸载前')
})

onUnmounted(() => {
  console.log('组件已卸载')
})
</script>
```

## 3. 组件系统

### 3.1 组件注册

```vue
<script setup>
// 局部注册
import MyComponent from './MyComponent.vue'
import AnotherComponent from './AnotherComponent.vue'
</script>

<template>
  <MyComponent />
  <AnotherComponent />
</template>
```

### 3.2 Props

```vue
<!-- ChildComponent.vue -->
<script setup>
const props = defineProps({
  title: {
    type: String,
    required: true
  },
  count: {
    type: Number,
    default: 0
  },
  user: {
    type: Object,
    default: () => ({})
  }
})

// 使用 props
console.log(props.title)
</script>
```

```vue
<!-- ParentComponent.vue -->
<template>
  <ChildComponent
    title="Hello Vue 3"
    :count="10"
    :user="{ name: 'John' }"
  />
</template>
```

### 3.3 事件

```vue
<!-- ChildComponent.vue -->
<script setup>
const emit = defineEmits(['update', 'delete'])

const handleUpdate = () => {
  emit('update', { id: 1, name: 'Updated' })
}

const handleDelete = (id) => {
  emit('delete', id)
}
</script>
```

```vue
<!-- ParentComponent.vue -->
<template>
  <ChildComponent
    @update="handleUpdate"
    @delete="handleDelete"
  />
</template>

<script setup>
const handleUpdate = (data) => {
  console.log('Updated:', data)
}

const handleDelete = (id) => {
  console.log('Deleted:', id)
}
</script>
```

### 3.4 v-model

```vue
<!-- CustomInput.vue -->
<script setup>
const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const updateValue = (event) => {
  emit('update:modelValue', event.target.value)
}
</script>

<template>
  <input :value="modelValue" @input="updateValue" />
</template>
```

```vue
<!-- 使用 -->
<template>
  <CustomInput v-model="message" />
  <!-- 等同于 -->
  <CustomInput
    :modelValue="message"
    @update:modelValue="message = $event"
  />
</template>
```

多个 v-model：

```vue
<script setup>
const props = defineProps({
  title: String,
  content: String
})
const emit = defineEmits(['update:title', 'update:content'])
</script>
```

```vue
<template>
  <UserForm
    v-model:title="pageTitle"
    v-model:content="pageContent"
  />
</template>
```

### 3.5 插槽

```vue
<!-- LayoutComponent.vue -->
<template>
  <div class="layout">
    <header>
      <slot name="header" />
    </header>
    <main>
      <slot />
    </main>
    <footer>
      <slot name="footer" />
    </footer>
  </div>
</template>
```

```vue
<!-- 使用 -->
<template>
  <LayoutComponent>
    <template #header>
      <h1>页面标题</h1>
    </template>

    <p>主要内容</p>

    <template #footer>
      <p>版权信息</p>
    </template>
  </LayoutComponent>
</template>
```

作用域插槽：

```vue
<!-- ListComponent.vue -->
<script setup>
const items = ref(['Item 1', 'Item 2', 'Item 3'])
</script>

<template>
  <ul>
    <li v-for="item in items" :key="item">
      <slot :item="item" :index="index" />
    </li>
  </ul>
</template>
```

```vue
<!-- 使用 -->
<template>
  <ListComponent v-slot="{ item, index }">
    {{ index + 1 }}. {{ item }}
  </ListComponent>
</template>
```

### 3.6 动态组件

```vue
<script setup>
import ComponentA from './ComponentA.vue'
import ComponentB from './ComponentB.vue'

const currentComponent = ref('ComponentA')
const components = {
  ComponentA,
  ComponentB
}
</script>

<template>
  <button @click="currentComponent = 'ComponentA'">A</button>
  <button @click="currentComponent = 'ComponentB'">B</button>

  <component :is="components[currentComponent]" />
</template>
```

### 3.7 异步组件

```vue
<script setup>
import { defineAsyncComponent } from 'vue'

// 基本用法
const AsyncComponent = defineAsyncComponent(() =>
  import('./HeavyComponent.vue')
)

// 带加载状态和错误处理
const AsyncComponentWithOptions = defineAsyncComponent({
  loader: () => import('./HeavyComponent.vue'),
  loadingComponent: LoadingComponent,
  errorComponent: ErrorComponent,
  delay: 200,
  timeout: 3000
})
</script>
```

## 4. 组合式函数 (Composables)

### 4.1 创建 Composable

```javascript
// useMouse.js
import { ref, onMounted, onUnmounted } from 'vue'

export function useMouse() {
  const x = ref(0)
  const y = ref(0)

  const update = (event) => {
    x.value = event.pageX
    y.value = event.pageY
  }

  onMounted(() => window.addEventListener('mousemove', update))
  onUnmounted(() => window.removeEventListener('mousemove', update))

  return { x, y }
}
```

```vue
<script setup>
import { useMouse } from './useMouse'

const { x, y } = useMouse()
</script>

<template>
  <p>Mouse position: {{ x }}, {{ y }}</p>
</template>
```

### 4.2 常用 Composables

```javascript
// useFetch.js
import { ref, watchEffect, toValue } from 'vue'

export function useFetch(url) {
  const data = ref(null)
  const error = ref(null)
  const loading = ref(false)

  const fetchData = async () => {
    loading.value = true
    error.value = null

    try {
      const res = await fetch(toValue(url))
      data.value = await res.json()
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  watchEffect(() => {
    fetchData()
  })

  return { data, error, loading, refresh: fetchData }
}
```

```javascript
// useLocalStorage.js
import { ref, watch } from 'vue'

export function useLocalStorage(key, defaultValue) {
  const stored = localStorage.getItem(key)
  const data = ref(stored ? JSON.parse(stored) : defaultValue)

  watch(data, (newValue) => {
    localStorage.setItem(key, JSON.stringify(newValue))
  }, { deep: true })

  return data
}
```

## 5. 状态管理

### 5.1 Pinia（推荐）

安装：

```bash
npm install pinia
```

创建 Store：

```javascript
// stores/counter.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCounterStore = defineStore('counter', () => {
  // State
  const count = ref(0)
  const name = ref('Counter')

  // Getters
  const doubleCount = computed(() => count.value * 2)

  // Actions
  function increment() {
    count.value++
  }

  function decrement() {
    count.value--
  }

  return { count, name, doubleCount, increment, decrement }
})
```

使用 Store：

```vue
<script setup>
import { useCounterStore } from '@/stores/counter'

const counter = useCounterStore()
</script>

<template>
  <div>
    <p>Count: {{ counter.count }}</p>
    <p>Double: {{ counter.doubleCount }}</p>
    <button @click="counter.increment">+</button>
    <button @click="counter.decrement">-</button>
  </div>
</template>
```

### 5.2 Provide / Inject

```vue
<!-- App.vue -->
<script setup>
import { provide, ref } from 'vue'

const user = ref({
  name: 'John',
  role: 'admin'
})

provide('user', user)
</script>
```

```vue
<!-- DeepChild.vue -->
<script setup>
import { inject } from 'vue'

const user = inject('user')
const defaultUser = inject('user', { name: 'Guest' })
</script>
```

## 6. 路由 - Vue Router 4

### 6.1 基本配置

```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/About.vue')
  },
  {
    path: '/user/:id',
    name: 'User',
    component: () => import('../views/User.vue'),
    children: [
      {
        path: 'profile',
        component: () => import('../views/UserProfile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

### 6.2 路由导航

```vue
<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 编程式导航
const goToUser = (id) => {
  router.push(`/user/${id}`)
  // 或
  router.push({ name: 'User', params: { id } })
}

// 获取路由参数
console.log(route.params.id)
console.log(route.query.search)
</script>
```

### 6.3 路由守卫

```javascript
// 全局前置守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

// 路由独享守卫
{
  path: '/admin',
  component: Admin,
  beforeEnter: (to, from) => {
    // 返回 false 取消导航
    return isAdmin()
  }
}
```

## 7. 动画与过渡

### 7.1 过渡组件

```vue
<template>
  <button @click="show = !show">Toggle</button>

  <Transition name="fade">
    <p v-if="show">Hello</p>
  </Transition>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
```

### 7.2 列表过渡

```vue
<template>
  <TransitionGroup name="list" tag="ul">
    <li v-for="item in items" :key="item.id">
      {{ item.name }}
    </li>
  </TransitionGroup>
</template>

<style>
.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
```

## 8. 性能优化

### 8.1 虚拟列表

```vue
<script setup>
import { ref, computed } from 'vue'

const items = ref(Array.from({ length: 10000 }, (_, i) => ({
  id: i,
  text: `Item ${i}`
})))

const itemHeight = 50
const containerHeight = 400
const visibleCount = Math.ceil(containerHeight / itemHeight)

const scrollTop = ref(0)
const startIndex = computed(() => Math.floor(scrollTop.value / itemHeight))
const endIndex = computed(() => startIndex.value + visibleCount)
const visibleItems = computed(() =>
  items.value.slice(startIndex.value, endIndex.value)
)
const offsetY = computed(() => startIndex.value * itemHeight)

const onScroll = (e) => {
  scrollTop.value = e.target.scrollTop
}
</script>

<template>
  <div class="viewport" @scroll="onScroll">
    <div class="list" :style="{ height: items.length * itemHeight + 'px' }">
      <div class="visible-list" :style="{ transform: `translateY(${offsetY}px)` }">
        <div
          v-for="item in visibleItems"
          :key="item.id"
          class="item"
          :style="{ height: itemHeight + 'px' }"
        >
          {{ item.text }}
        </div>
      </div>
    </div>
  </div>
</template>
```

### 8.2 懒加载

```vue
<script setup>
import { defineAsyncComponent } from 'vue'

const HeavyComponent = defineAsyncComponent(() =>
  import('./HeavyComponent.vue')
)
</script>
```

### 8.3 v-once 和 v-memo

```vue
<template>
  <!-- 只渲染一次 -->
  <div v-once>{{ staticContent }}</div>

  <!-- 只在依赖变化时重新渲染 -->
  <div v-memo="[valueA, valueB]">
    {{ valueA }} {{ valueB }} {{ valueC }}
  </div>
</template>
```

---

本文档全面介绍了 Vue 3 的核心概念和用法，包括组合式 API、组件系统、状态管理、路由、动画等内容，是 Vue 3 开发的完整参考指南。
