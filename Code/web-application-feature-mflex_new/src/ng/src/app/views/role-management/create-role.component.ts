import {Component} from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Observable} from "rxjs";
import {App} from "../../app.model";
import {map} from "rxjs/operators";
import {Role} from "./role.model";
import {NgForm} from "@angular/forms";
import {Router} from "@angular/router";

@Component({
    selector: 'bordered-content-wrapper',
    templateUrl: 'create-role.component.html'
})
export class CreateRoleComponent extends FormHandlerUtil<Role[]> {
    public nullReference: any = null
    public roles: Role[]

    public name: string
    public inherit: number = this.nullReference

    public formSubmissionWaitingForValidationCompletion: NgForm = null

    constructor(private router: Router) {
        super("/role/inheritableRoles", "/role/createRole")
        this.submit.subscribe(x => {
            if(x.code == 200) {
                this.router.navigateByUrl("/role-management/role-list")
            }
        })
    }

    protected onLoadData(x: Role[]) {
        this.roles = x
    }

    public onSubmit(form: NgForm) {
        if (form.form.pending) {
            this.formSubmissionWaitingForValidationCompletion = form
        } else {
            super.onSubmit(form)
        }
    }

    public duplicateNameChecker(value): Observable<any> {
        let data = new FormData()
        data.append("name", value)
        return this.http.post<boolean>(App.basePath + "/role/isNameExist", data).pipe(map(x => {
            if (x) {
                if (this.formSubmissionWaitingForValidationCompletion) {
                    this.formSubmissionWaitingForValidationCompletion = null
                }
                return true;
            }
            if (this.formSubmissionWaitingForValidationCompletion) { //If any form submission pending - then on success call again
                this.onSubmit(this.formSubmissionWaitingForValidationCompletion)
            }
            return null
        }))
    }
}