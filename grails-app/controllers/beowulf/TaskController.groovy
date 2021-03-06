package beowulf

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
@Secured('ROLE_USER')
class TaskController {
    def springSecurityService
    def taskService


    static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

    def index(Project project) {
        def tasks = taskService.findTasksBy(project,params)


        render view:'index',model:[taskCount: tasks.size(),tasks:tasks,project:project]
    }
    def close(Task task){
        respond task
    }


    def show(Task task) {
        respond task
    }

    def create(Project project) {
        respond new Task(params),model:[project:project]
    }

    @Transactional
    def save(Task task) {
        def _loggedUser = springSecurityService.getPrincipal()
        def _user = User.findByUsername(_loggedUser.username)
        def _project = Project.get(params.projectId)
        if (task == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }
        if(task.assignedTo){
            task.status = Status.ASSIGNED
        }
        task.createdDate = new Date()
        task.openBy = _user
        task.project = _project
        _project.addToTasks(task)

        if (!task.validate()) {
            transactionStatus.setRollbackOnly()
            respond task.errors, view:'create', model:[project: _project]
            return
        }

        //task.save flush:true, failOnError:true
        _project.save(flush:true,failOnError:true)

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'task.label', default: 'Task'), task.id])
                redirect controller: 'project', action: 'dashboard', id: task.project.id
            }
            '*' { respond task, [status: CREATED] }
        }
    }

    def edit(Task task) {
        respond task, model:[project: task.project]
    }
    def myTasks(){
        def _loggedUser = springSecurityService.getPrincipal()
        def _user = User.findByUsername(_loggedUser.username)


    }

    @Transactional
    def update(Task task) {
        if (task == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (!task.validate()) {
            transactionStatus.setRollbackOnly()
            respond task.errors, view:'edit'
            return
        }

        task.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = "Tarefa salva com sucesso"
                redirect controller: 'task', action: 'show', id: task.id
            }
            '*'{ respond task, [status: OK] }
        }
    }

    @Transactional
    def delete(Task task) {

        if (task == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        task.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'task.label', default: 'Task'), task.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }
    def doClose(Task task){
        task.closeDate = new Date()
        task.status = Status.CLOSED
        task.save(flush:true)

        flash.message = "Tarefa fechada com sucesso"
        redirect controller: "task", action: "show" , id: task.id
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'task.label', default: 'Task'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
