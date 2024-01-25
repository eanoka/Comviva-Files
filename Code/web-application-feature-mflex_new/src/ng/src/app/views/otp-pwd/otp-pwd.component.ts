import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators} from '@angular/forms';
import { Router } from '@angular/router';
import { LoginSharedService } from '../service/login-shared.service';
import { ForgotpasswordService } from '../service/forgotpassword.service';
import { LoginService } from '../service/login.service';

@Component({
  selector: 'app-otp-pwd',
  templateUrl: './otp-pwd.component.html',
  styleUrls: ['./otp-pwd.component.css']
})
export class OtpPwdComponent {
  OTPAuth: FormGroup;
  errormsg:boolean=true;
  receivedRequestID: any;

  constructor(private formBuilder: FormBuilder,private router: Router, private loginApiService: LoginService, private dataService: LoginSharedService, private forgotPWDApiService: ForgotpasswordService) {
    this.OTPAuth = this.formBuilder.group({
      otpAuth: ['', [Validators.required]]
    });
  }

  ngOnInit() {
    this.dataService.serviceID$.subscribe(data => {
      console.log('data------------->',data)
      this.receivedRequestID = data;
    });
  }

  verifyOTP() {
    const OTP = this.OTPAuth.get('otpAuth')?.value;
    console.log('OTP :', OTP);
    console.log('Service request ID from OTP :', this.receivedRequestID);
    // this.router.navigate(['/new-password']);
    this.loginApiService.getOtp('SELFSETAUTHMFA','01732112233').subscribe((data: any) => {
    this.forgotPWDApiService.validateOtp(OTP,this.receivedRequestID).subscribe(
      (response)=>{
        console.log('Validate OTP error Response: ',response)
        this.router.navigate(['/new-password']);
      },(error)=>{
        console.log('Validate OTP error Response: ',error.error);
        const errorMsg= error.error.errors[0].message;
        console.log('Error message: ',errorMsg);
        alert(errorMsg)
        this.router.navigate(['/new-password']);
      }
    )
  })
  }
  
}
