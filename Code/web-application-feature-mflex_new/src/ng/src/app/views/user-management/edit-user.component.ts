import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {UserFilterComponentBase} from "./user-filter-component-base";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'user-filter-component-base.html'
})
export class EditUserComponent extends UserFilterComponentBase {
  constructor(private router: Router) {
    super("/user/editableUsers", undefined, false)
    this.op = 'Edit'
  }

  onSubmit(form: any) {
    this.router.navigateByUrl("/user-management/create-user", {state: form.value})
  }
}