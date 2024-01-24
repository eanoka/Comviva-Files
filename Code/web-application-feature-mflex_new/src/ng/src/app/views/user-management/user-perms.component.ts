import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Actions} from "../../generic/actions.consts";
import {App as app, App} from "../../app.model";
import {Snackbar} from "../blocks/snackbar.component";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {User} from "./user.model";
import {AccessiblePermissionsResponse} from "./accessible-permissions-response.model";
import {AllowDeny} from "./action.model";

interface Changes {
  [key: string]: string
}

class RoleInView {
  public id: number
  public name: string
  public users: User[]
}

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'user-perms.component.html',
  styleUrls: ["../role-management/role-perms.component.scss"]
})
export class UserPermsComponent extends FormHandlerUtil<User[]> {
  public nullReference: any = null
  public allowDenyEquality: Function = AllowDeny.prototype.equals
  public editPermission = Actions.EDIT_PERMISSION
  public fixedActions: any = Object.values(Actions)
  public renderableActions: string[]
  public ownPermissions: string[] = App.user.permissions
  public roles: RoleInView[]
  public users: User[]
  public permissions: AccessiblePermissionsResponse
  public displayablePermissions: AllowDeny[]

  public editMode: boolean = false
  public roleId: number = this.nullReference
  public userId: number = this.nullReference

  public changes: Changes = {}

  constructor() {
    super("/user/permissionModifiableUsers", "/user/getPermissions", false)
    this.submit.subscribe(x => {
      this.permissions = x
      this.renderableActions = [...this.fixedActions, ...x.customActions.map(c => c.name)]
      this.editMode = false
      this.displayablePermissions = this.permissions.cumulative
      this.changes = {}
    })
  }

  public onChangeRole() {
    this.userId = this.nullReference
    this.users = this.roles.findSimilar(this.roleId, function(t) {
      return this.id == t
    }).users
  }

  protected onLoadData(x: User[]) {
    let cache = {}
    for(let y of x) {
      let rv: RoleInView = cache[y.role.id] || (cache[y.role.id] = {users: []})
      rv.id = y.role.id
      rv.name = y.role.name
      rv.users.push(y)
    }
    this.roles = Object.values(cache)
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
    this.http.post<ApiPostResponse>(app.basePath + "/user/updatePermissions", {id: this.permissions.user.id, changes: this.changes}).subscribe(x => {
      Snackbar.show(x.code == 200 ? "success" : "danger", x.message);
      this.submitting = false
      this.changes = {}
      this.onSubmit({value: {id: this.userId}, valid: true})
    }, err => {
      this.submitting = false
    })
  }
}