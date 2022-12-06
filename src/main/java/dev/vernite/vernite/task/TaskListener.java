/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.task;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import dev.vernite.protobuf.BasicAction;
import dev.vernite.protobuf.Task.Builder;
import dev.vernite.vernite.ws.SocketHandler;

public class TaskListener {

    private static Builder serialize(Task task) {
        return dev.vernite.protobuf.Task.newBuilder()
            .setName(task.getName())
            .setDescription(task.getDescription())
            .setCreatedAt(task.getCreatedAt().getTime())
            .setType(task.getType())
            .setPriority(task.getPriority())
            .setStatusId(task.getStatus().getId())
            .setCreatedBy(task.getCreatedBy())
            .setProjectId(task.getStatus().getProject().getId())
            .setId(task.getId());
    }

    @PostPersist
    private void postPersist(Task task) {
        SocketHandler.bc(serialize(task).setAction(BasicAction.ADDED).build());
    }

    @PostUpdate
    private void postUpdate(Task task) {
        SocketHandler.bc(serialize(task).setAction(BasicAction.UPDATED).build());
    }

    @PostRemove
    private void postRemove(Task task) {
        SocketHandler.bc(serialize(task).setAction(BasicAction.REMOVED).build());
    }
}
