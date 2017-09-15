/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.ws.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeEnum;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.net.ws.Constants;
import org.ballerinalang.net.ws.WebSocketConnectionManager;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.io.IOException;
import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Get the ID of the connection.
 *
 * @since 0.94
 */

@BallerinaFunction(
        packageName = "ballerina.net.ws",
        functionName = "closeConnection",
        args = {@Argument(name = "conn", type = TypeEnum.STRUCT, structType = "Connection",
                          structPackage = "ballerina.net.ws"),
                @Argument(name = "statusCode", type = TypeEnum.INT),
                @Argument(name = "reason", type = TypeEnum.STRING)},
        isPublic = true
)
public class CloseConnection extends AbstractNativeFunction {

    @Override
    public BValue[] execute(Context context) {

        if (context.getServiceInfo() == null ||
                !context.getServiceInfo().getProtocolPkgPath().equals(Constants.WEBSOCKET_PACKAGE_NAME)) {
            throw new BallerinaException("This function is only working with WebSocket services");
        }

        BStruct wsConnection = (BStruct) getRefArgument(context, 0);
        int statusCode = (int) getIntArgument(context, 0);
        String reason = getStringArgument(context, 0);
        Session session = (Session) wsConnection.getNativeData(Constants.NATIVE_DATA_WEBSOCKET_SESSION);
        try {
            session.close(new CloseReason(() -> statusCode, reason));
        } catch (IOException e) {
            throw new BallerinaException("Could not close the connection: " + e.getMessage());
        } finally {
            WebSocketConnectionManager.getInstance().removeConnection(session.getId());
        }
        return VOID_RETURN;
    }
}
