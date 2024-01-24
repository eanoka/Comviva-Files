import { Component } from '@angular/core';
import {Navigation, Router} from "@angular/router";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {CreateFormInitializationData} from "./create-form-initialization-data.model";
import {App} from "../../app.model";
import {SessionUser} from "./session-user.model";
import {Client} from "../manage-bill-data/client.model";
import {Actions} from "../../generic/actions.consts";
import {Role} from "../role-management/role.model";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {NgForm} from "@angular/forms";

interface SubmissionModel {
  id: number
  account: Client
  divisions: string[]
  name: string
  mobile: string
  email: string
  address: string
  role: Role
  adid: string
}

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'create-user.component.html',
  styles: ["form > div:first-child {\n" +
  "    text-align: center;\n" +
  "    font-size: x-large;\n" +
  "    font-weight: bold;\n" +
  "}form > div:first-child > div {outline: 2px solid #000;}"]
})
export class CreateUserComponent extends FormHandlerUtil<CreateFormInitializationData> {
  public model: CreateFormInitializationData = {} as CreateFormInitializationData
  public loggedUser: SessionUser = App.user
  public boundModel: SubmissionModel
  public nullReference: any = null
  public Actions: any = Actions
  public divisionEntries: any[]
  public permissions: string[] = App.user.permissions

  public formSubmissionWaitingForValidationCompletion: NgForm = null

  constructor(private router: Router) {
    super("/user/loadCreateFormEntities" + CreateUserComponent.getPreLoadRequestParams(router.getCurrentNavigation()), undefined)
    this.dataSubmissionUrl = router.getCurrentNavigation()?.extras?.state?.user ? "/user/update" : "/user/create"
    this.boundModel = {} as SubmissionModel
    this.submit.subscribe(x => {
      if(x.code == 200) {
        this.router.navigateByUrl("/user-management/user-report")
      }
    })
  }

  public onSubmit(form: NgForm) {
    if (form.form.pending) {
      this.formSubmissionWaitingForValidationCompletion = form
    } else {
      super.onSubmit(form)
    }
  }

  protected prepareSubmissionData(v: any): any {
    v.accountId = v.account?.id
    return v
  }

  private static getPreLoadRequestParams(nav: Navigation): string {
    let state: any = nav?.extras?.state
    return state ? "?id=" + state.user.id : ""
  }

  protected onLoadData(x: CreateFormInitializationData) {
    this.model = x
    this.boundModel.id = this.model.user?.id
    this.boundModel.account = this.model.user?.client || this.nullReference
    this.boundModel.divisions = (this.model.user?.clientDivisions || []).map(v => "" + v.id)
    if(this.model.divisions) {
      this.divisionEntries = this.model.divisions
    }
    this.boundModel.name = this.model.user?.name
    this.boundModel.mobile = this.model.user ? "0" + this.model.user.msisdn : ""
    this.boundModel.email = this.model.user?.email
    this.boundModel.adid = this.model.user?.adid
    this.boundModel.address = this.model.user?.address
    this.boundModel.role = this.model.user?.role?.id || this.nullReference
  }

  public onSubAccountChange() {
    if(!this.boundModel.divisions.length) {
      this.boundModel.account = this.nullReference
    }
  }

  private checkDuplicate(url: string, data: FormData): Observable<any> {
    if(this.boundModel.id) {
      data.append("id", "" + this.boundModel.id)
    }
    return this.http.post<boolean>(App.basePath + url, data).pipe(map(x => {
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

  public duplicateMsisdnChecker(value): Observable<any> {
    let data = new FormData()
    data.append("msisdn", value)
    return this.checkDuplicate("/user/isMsisdnExist", data)
  }

  public duplicateEmailChecker(value): Observable<any> {
    let data = new FormData()
    data.append("email", value)
    return this.checkDuplicate("/user/isEmailExist", data)
  }

  public duplicateAdidChecker(value): Observable<any> {
    let data = new FormData()
    data.append("adid", value)
    return this.checkDuplicate("/user/isAdidExist", data)
  }
}