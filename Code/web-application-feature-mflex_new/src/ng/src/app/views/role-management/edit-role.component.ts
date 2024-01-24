import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Role} from "./role.model";
import {NgForm} from "@angular/forms";
import {Router} from "@angular/router";
import {Observable} from "rxjs";
import {App} from "../../app.model";
import {map} from "rxjs/operators";
import {Actions} from "../../generic/actions.consts";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'edit-role.component.html'
})
export class EditRoleComponent extends FormHandlerUtil<Role[]> {
  public nullReference: any = null
  public roles: Role[]
  public app: any = App
  public actions: any = Actions

  public roleId: number = this.nullReference
  public role: Role
  public name: string
  public inherit: Role = this.nullReference

  public formSubmissionWaitingForValidationCompletion: NgForm = null

  constructor(private router: Router) {
    super("/role/editableRoles", "/role/getRole", false)
    this.submit.subscribe(x => {
      if(x.code) {
        if(x.code == 200) {
          this.router.navigateByUrl("/role-management/role-list")
        }
        return;
      }
      this.role = x
      this.dataLoaded = false
      this.roles = []
      this.dataFetcherUrl = "/role/inheritableRoles"
      this.dataSubmissionUrl = "/role/updateRole"
      this.name = x.name
      this.inherit = x.inheritedFrom?.id || this.nullReference
      this.loadData()
    })
  }

  public backToSelection() {
    this.role = null
    this.dataLoaded = false
    this.dataFetcherUrl = "/role/editableRoles"
    this.dataSubmissionUrl = "/role/getRole"
    this.loadData()
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
    data.append("id", "" + this.role.id)
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