package com.workflow.workflow;

import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProjectControllerTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private ProjectRepository projectRepository;

    private Workspace workspace;
    private User user;

    @BeforeAll
    void init() {
        user = userRepository.findById(1L)
                .orElse(userRepository.save(new User("Name", "Surname", "Username", "Email", "Password")));
        workspace = workspaceRepository.save(new Workspace("name", user));
    }

    @BeforeEach
    void reset() {
        projectWorkspaceRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void addSuccess() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\": \"test\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("test")));
    }

    @Test
    void addNotFound() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"test\", \"workspaceId\": 700000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addBadRequest() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addUnsupportedMedia() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/project/").contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/project/").contentType(MediaType.MULTIPART_FORM_DATA)
                .content(String.format("{\"name\": \"test\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getSuccess() throws Exception {
        Project project = projectRepository.save(new Project("name 1"));

        mvc.perform(get(String.format("/project/%d", project.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(project.getName())))
                .andExpect(jsonPath("$.projectMembers", hasSize(0)));

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        mvc.perform(get(String.format("/project/%d", project.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(project.getName())))
                .andExpect(jsonPath("$.projectMembers", hasSize(1)))
                .andExpect(jsonPath("$.projectMembers[0].privileges", is(1)));
    }

    @Test
    void getNotFound() throws Exception {
        mvc.perform(get("/project/70000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchSuccess() throws Exception {
        Project project = projectRepository.save(new Project("patch"));

        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new patch\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new patch")));

        mvc.perform(get(String.format("/project/%d", project.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new patch")));
    }

    @Test
    void patchBadRequest() throws Exception {
        Project project = projectRepository.save(new Project("patch"));

        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchNotFound() throws Exception {
        mvc.perform(patch("/project/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new patch\"}"))
                .andExpect(status().isNotFound());

        Project project = projectRepository.save(new Project("patch not found"));

        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"new patch\", \"workspaceId\": 70000}"))
                .andExpect(status().isNotFound());

        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\": \"new patch\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchUnsupportedMedia() throws Exception {
        Project project = projectRepository.save(new Project("patch 415"));

        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(patch(String.format("/project/%d", project.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void putSuccess() throws Exception {
        Project project = projectRepository.save(new Project("put"));

        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\": \"new put\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));

        mvc.perform(get(String.format("/project/%d", project.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));

        mvc.perform(put("/project/7000").contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\": \"new put 2\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put 2")));
    }

    @Test
    void putBadRequest() throws Exception {
        Project project = projectRepository.save(new Project("put"));

        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/project/5000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/project/5111")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putNotFound() throws Exception {
        Project project = projectRepository.save(new Project("put"));

        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"new put\", \"workspaceId\": 5656}"))
                .andExpect(status().isNotFound());

        mvc.perform(put("/project/77")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"new put\", \"workspaceId\": 5656}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putUnsupportedMedia() throws Exception {
        Project project = projectRepository.save(new Project("put"));

        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d", project.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .content(String.format("{\"name\": \"test\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isUnsupportedMediaType());

        mvc.perform(put("/project/777").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put("/project/777").contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put("/project/777").contentType(MediaType.MULTIPART_FORM_DATA)
                .content(String.format("{\"name\": \"test\", \"workspaceId\": %d}", workspace.getId())))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void deleteSuccess() throws Exception {
        Project project = projectRepository.save(new Project("put"));

        mvc.perform(delete(String.format("/project/%d", project.getId())))
                .andExpect(status().isOk());

        Project project1 = projectRepository.save(new Project("put"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project1, workspace, 1L));

        mvc.perform(delete(String.format("/project/%d", project1.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete("/project/77777"))
                .andExpect(status().isNotFound());
    }
}