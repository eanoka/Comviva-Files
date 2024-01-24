import {Component} from '@angular/core';
import {UserFilterComponentBase} from "./user-filter-component-base";
import {Router} from "@angular/router";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'user-filter-component-base.html'
})
export class BarUnbarUserComponent extends UserFilterComponentBase {
  constructor(private router: Router) {
    super("/user/barUnbarrableUsers", "/user/toggleActive", false)
    this.confirmationRequired = true
    this.confirmationMessage = (form: any) => "This user will be " + (form.value.user.active ? "barred" : "unbarred") + ". Do you want to proceed?"
    this.submit.subscribe(x => {
      if(x.code == 200) {
        this.router.navigateByUrl("/user-management/user-report")
      }
    })
  }
}