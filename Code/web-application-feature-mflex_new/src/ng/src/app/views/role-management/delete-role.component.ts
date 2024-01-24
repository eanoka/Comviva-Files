import {Component} from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Role} from "./role.model";
import {App} from "../../app.model";
import {Actions} from "../../generic/actions.consts";
import {Router} from "@angular/router";

@Component({
    selector: 'bordered-content-wrapper',
    templateUrl: 'delete-role.component.html'
})
export class DeleteRoleComponent extends FormHandlerUtil<Role[]> {
    public nullReference: any = null
    public roles: Role[]
    public app: any = App
    public actions: any = Actions

    public roleId: number = this.nullReference
    public role: Role
    public name: string

    constructor(private router: Router) {
        super("/role/deletableRoles", "/role/deleteRole", false)
        this.confirmationRequired = true
        this.confirmationMessage = "Do you really want to delete? This process can not be undone."
        this.submit.subscribe(x => {
            if (x.code == 200) {
                this.router.navigateByUrl("/role-management/role-list")
            }
        })
    }

    protected onLoadData(x: Role[]) {
        this.roles = x
    }
}