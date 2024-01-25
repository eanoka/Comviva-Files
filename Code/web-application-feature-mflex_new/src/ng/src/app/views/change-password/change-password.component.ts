import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { ChangepaswwordService } from '../service/changepassword.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css']
})
export class ChangePasswordComponent {

  changepassword: FormGroup;
  errormsg: boolean = false;
  isPasswordVisible: boolean = false;
  constructor(private formBuilder: FormBuilder, private changePasswordAPIservice: ChangepaswwordService, private router: Router) {
    this.changepassword = this.formBuilder.group({
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required]],
      confirmNewPassword: ['', [Validators.required]],
    });
  }

  submitChangePassword() {
    const oldPassword = this.changepassword.get('oldPassword')?.value;
    const newPassword = this.changepassword.get('newPassword')?.value;
    const confirmPassword = this.changepassword.get('confirmNewPassword')?.value
    console.log('old password:', oldPassword);
    console.log('new password:', newPassword);
    console.log('confirm new password:', confirmPassword);
    if (newPassword === confirmPassword) {
      this.changePasswordAPIservice.changeCredential(oldPassword, newPassword, confirmPassword).subscribe(
        (response) => {
          console.log('Change password API success response: ',response);
          this.router.navigate(['']);
          localStorage.removeItem('token');
          alert('changed password successfully');
        }, (error) => {
          console.log('Change password API error response: ',error);
          const errorMsg= error.error.errors[0].message;
          alert(errorMsg);
        }
      )
    } else {
      console.log('Passwords do not match.');
      alert('New and confirm Passwords do not match.')
      this.errormsg = true
    }
  }

}
