package com.Mario.allis.core.orchestration

import java.util.UUID

data class OrchestratedTask(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val priority: TaskPriority,
    val requiresConfirmation: Boolean,
    var status: TaskStatus = TaskStatus.PENDING
)

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class TaskStatus {
    PENDING,
    ACTIVE,
    COMPLETE,
    FAILED
}

class TaskOrchestrator {

    private val tasks = mutableListOf<OrchestratedTask>()

    fun addTask(task: OrchestratedTask) {
        tasks.add(task)
    }

    fun nextTask(): OrchestratedTask? {
        return tasks.filter { it.status == TaskStatus.PENDING }
            .sortedWith(compareByDescending<OrchestratedTask> { it.priority }
                .thenBy { it.description.length })
            .firstOrNull()
    }

    fun markActive(taskId: String) {
        tasks.find { it.id == taskId }?.status = TaskStatus.ACTIVE
    }

    fun complete(taskId: String) {
        tasks.find { it.id == taskId }?.status = TaskStatus.COMPLETE
    }

    fun fail(taskId: String) {
        tasks.find { it.id == taskId }?.status = TaskStatus.FAILED
    }

    fun pendingSummary(): String {
        val pending = tasks.count { it.status == TaskStatus.PENDING }
        return "Pendientes: $pending"
    }
}
