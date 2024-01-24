import { Component } from '@angular/core';
import {UserFilterComponentBase} from "./user-filter-component-base";
import {Router} from "@angular/router";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'user-filter-component-base.html'
})
export class DeleteUserComponent extends UserFilterComponentBase {
  constructor(private router: Router) {
    super("/user/deletableUsers", "/user/delete", false)
    this.op = 'Delete'
    this.confirmationRequired = true
    this.confirmationMessage = "Do you really want to delete? This process can not be undone."
    this.submit.subscribe(x => {
      if(x.code == 200) {
        this.router.navigateByUrl("/user-management/user-report")
      }
    })
  }
}