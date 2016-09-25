package beowulf

import grails.converters.JSON


import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.plugin.springsecurity.annotation.Secured


@Transactional(readOnly = true)
@Secured('ROLE_USER')

class ProjectController {
    def springSecurityService

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        def loggedUser = springSecurityService.getPrincipal()
        def user = User.findByUsername(loggedUser.username)
        def projectList = Project.findAllByOwner(user)

        params.max = Math.min(max ?: 10, 100)
        respond projectList, model:[projectCount: projectList.size()]
    }

    def dashboard(Project project){
        def _memberList = project.members - project.owner

        respond project
    }

    def show(Project project) {
        respond project
    }

    def create() {

        respond new Project(params)
    }

    @Transactional
    def save(Project project) {
        def loggedUser = springSecurityService.getPrincipal()
        def user = User.findByUsername(loggedUser.username)


        if (project == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }
        project.owner = user

        if (!project.validate()) {
            transactionStatus.setRollbackOnly()
            respond project.errors, view:'create'
            return
        }
        project.addToMembers(user)
        project.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'project.label', default: 'Project'), project.id])
                redirect action: 'dashboard', id: project.id
            }
            '*' { respond project, [status: CREATED] }
        }
    }

    def edit(Project project) {
        respond project
    }

    @Transactional
    def update(Project project) {
        if (project == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (project.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond project.errors, view:'edit'
            return
        }

        project.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'project.label', default: 'Project'), project.id])
                redirect project
            }
            '*'{ respond project, [status: OK] }
        }
    }

    @Transactional
    def delete(Project project) {

        if (project == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        project.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'project.label', default: 'Project'), project.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'project.label', default: 'Project'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }

    def addMember(Project project){
        respond project

    }
    def findMembers(){
        JSON.registerObjectMarshaller(User) {
            return [value:it.username, data:it.id,firstName:it.firstName, lastName:it.lastName ]
        }
        def query = params.query
        def results = User.findAllByUsernameIlike("%${query}%")

        response.setContentType("application/json")

        def responseData = [
                "suggestions" : results
        ]
        render responseData as JSON
    }
    def refreshMembers(Project project){
        def _memberList = []
        params.member.each{
            def _user = User.get(it)
            _memberList.add(_user)
        }
        project.addToMembers(_memberList)
        project.save(flush:true,failOnError:true)
        redirect action: 'dashboard', id: project.id


    }
    def removeMember(Project project){
        def _member = User.get(params.memberId)
        project.removeFromMembers(_member)
        project.save(flush:true)
        flash.message = "Membro removido com sucesso"
        redirect action: 'dashboard' , id: project.id


    }
}
