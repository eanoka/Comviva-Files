import {Component} from '@angular/core';
import {NgForm} from "@angular/forms";
import {HttpClient} from "@angular/common/http";
import {App} from "../../app.model";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {Snackbar} from "../blocks/snackbar.component";

@Component({
    templateUrl: 'create-account.component.html',
    selector: 'bordered-content-wrapper'
})
export class CreateAccountComponent {
    public name: string
    public mobileNo: string
    public address1: string
    public address2: string
    public description: string

    public submitting: boolean = false
    public formSubmissionWaitingForValidationCompletion: NgForm = null

    public App: any = App

    constructor(private http: HttpClient) {}

    public onSubmit(form: NgForm) {
        if (form.form.pending) {
            this.formSubmissionWaitingForValidationCompletion = form
        } else {
          this.http.post<ApiPostResponse>(App.basePath + "/account/createAccount", form.value).subscribe(x => {
              Snackbar.show(x.code == 200 ? "success" : "danger", x.message)
          })
        }
    }

    public duplicateNameChecker(value): Observable<any> {
        let data = new FormData()
        data.append("name", value)
        return this.http.post<boolean>(App.basePath + "/account/isNameExist", data).pipe(map(x => {
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
        return this.http.post<boolean>(App.basePath + "/account/isMsisdnExist", data).pipe(map(x => {
            if (x) {
                if (this.formSubmissionWaitingForValidationCompletion) {
                    this.formSubmissionWaitingForValidationCompletion = null
                }
                return true;
            }
            if (this.formSubmissionWaitingForValidationCompletion) {
                this.onSubmit(this.formSubmissionWaitingForValidationCompletion)
            }
            return null
        }))
    }
}