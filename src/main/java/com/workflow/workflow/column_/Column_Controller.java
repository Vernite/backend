package com.workflow.workflow.column_;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/project/{projectID}/column_")
public class Column_Controller {

    private static final String PROJECT_NOT_FOUND = "project not found";
    private static final String COLUMN_NOT_FOUND = "column not found";

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private Column_Repository column_Repository;

    @Operation(summary = "Retrive all columns.", description = "This method returns array of all columns for project with given ID. Result can be empty array. Throws status 404 when project with given ID does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all project columns. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Column_.class)))
            }),
            @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content())
    })
    @GetMapping("/")
    public Iterable<Column_> all(@PathVariable long projectID) {
        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
        return project.getColumns();
    }

    @Operation(summary = "Retrive column.", description = "This method is used to retrive column with given ID. On sucess returns column with given ID. Throws 404 when project or column does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project with given id.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Column_.class))
            }),
            @ApiResponse(responseCode = "404", description = "Project with given id not found.", content = @Content())
    })
    @GetMapping("/{id}")
    public Column_ get(@PathVariable long projectID, @PathVariable long id) {
        Column_ col = column_Repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, COLUMN_NOT_FOUND));
        if (col.getProject().getId() != projectID) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COLUMN_NOT_FOUND);
        }
        return col;
    }

    @Operation(summary = "Delete column.", description = "This method is used to delete column. On sucess does not return anything. Throws 404 when column or project does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Column with given ID has been deleted."),
            @ApiResponse(responseCode = "404", description = "Project or column with given ID not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long projectID, @PathVariable long id) {
        Column_ col = column_Repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, COLUMN_NOT_FOUND));
        if (col.getProject().getId() != projectID) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COLUMN_NOT_FOUND);
        }
        column_Repository.delete(col);
    }
}
