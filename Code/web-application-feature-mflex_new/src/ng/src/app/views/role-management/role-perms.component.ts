import { Component } from '@angular/core';
import {Actions} from "../../generic/actions.consts";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Role} from "./role.model";
import {PaginatedRole} from "./paginated-role.model";
import {AccessiblePermissionsResponse} from "./accessible-permissions-response.model";
import {App} from "../../app.model";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {Snackbar} from "../blocks/snackbar.component";
import {SessionUser} from "../user-management/session-user.model";
import {AllowDeny} from "../user-management/action.model";

interface Changes {
  [key: string]: string
}

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'role-perms.component.html',
  styleUrls: ["role-perms.component.scss"]
})
export class RolePermsComponent extends FormHandlerUtil<PaginatedRole> {
  public nullReference: any = null
  public allowDenyEquality: Function = AllowDeny.prototype.equals
  public editPermission = Actions.EDIT_PERMISSION
  public fixedActions: any = Object.values(Actions)
  public renderableActions: string[]
  public sessionUser: SessionUser = App.user
  public ownPermissions: string[] = App.user.permissions
  public roles: Role[]
  public permissions: AccessiblePermissionsResponse
  public displayablePermissions: AllowDeny[]

  public editMode: boolean = false
  public roleId: number = this.nullReference

  public changes: Changes = {}

  constructor() {
    super("/role/listableRoles", "/role/getPermissions", false)
    this.submit.subscribe(x => {
      this.permissions = x
      this.renderableActions = [...this.fixedActions, ...x.customActions.map(c => c.name)]
      this.editMode = false
      this.displayablePermissions = this.permissions.cumulative
      this.changes = {}
    })
  }

  protected onLoadData(x: PaginatedRole) {
    this.roles = x.records
  }

  public reselectRole() {
    this.permissions = undefined
    this.loadData()
  }

  public changeMode() {
    if(this.editMode) {
      this.changes = {}
      this.displayablePermissions = this.permissions.own
    } else {
      this.displayablePermissions = this.permissions.cumulative
    }
  }

  public onInputChange(ev, permission) {
    this.changes[permission] = ev.target.value
  }

  public updateChanges() {
    if(Object.keys(this.changes).length == 0) {
      Snackbar.show("warning", "No Changes To Update")
      return
    }
    this.submitting = true
    this.http.post<ApiPostResponse>(App.basePath + "/role/updatePermissions", {id: this.permissions.role.id, changes: this.changes}).subscribe(x => {
      Snackbar.show(x.code == 200 ? "success" : "danger", x.message);
      this.submitting = false
      this.changes = {}
      this.onSubmit({value: {id: this.roleId}, valid: true})
    }, err => {
      this.submitting = false
    })
  }
}