import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators} from '@angular/forms';
import { ForgotpasswordService } from '../service/forgotpassword.service';
import { Router } from '@angular/router';
import { LoginSharedService } from '../service/login-shared.service';

@Component({
  selector: 'app-forgotpassword',
  templateUrl: './forgotpassword.component.html',
  styleUrls: ['./forgotpassword.component.css']
})
export class ForgotpasswordComponent {

  verifyUserID: FormGroup;


  constructor(private formBuilder: FormBuilder, private forgotPWDApiService: ForgotpasswordService,private router: Router, private dataService: LoginSharedService) {
    this.verifyUserID = this.formBuilder.group({
      userID: ['', [Validators.required]]
    });
  }

  onVerifyUserID() {
    const userID = this.verifyUserID.get('userID')?.value;
    console.log('userID :', userID);
    this.forgotPWDApiService.getToken().subscribe(
      (response)=>{
        console.log('Fetch token API success response: ',response);
        const accessToken = response.access_token;
        console.log('access token value: ', accessToken);
        this.forgotPWDApiService.verifyUserID(accessToken,userID).subscribe(
          (res)=>{
            console.log('Verify userID API success response: ',res);
            console.log('Message: ',res.message);
            const status = res.status;
            const serviceRequestID = res.serviceRequestId;
            console.log('Status: ', status);
            console.log('Service Request ID: ', serviceRequestID);
            if (status == 'PAUSED'){
              alert(res.message)
              this.router.navigate(['otp-pwd']);
              this.dataService.setServiceID(serviceRequestID);
            }else{

            }
            
          },(err)=>{
            console.log('Verify userID API error response: ',err);
          }
        )
      },(error)=>{
        console.log('Fetch token API error response: ',error);
      }
    )

  }

}
