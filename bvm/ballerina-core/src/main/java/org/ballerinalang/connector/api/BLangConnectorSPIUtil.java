/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ballerinalang.connector.api;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.connector.impl.ConnectorSPIModelHelper;
import org.ballerinalang.connector.impl.ServiceImpl;
import org.ballerinalang.model.types.BServiceType;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BTypeValue;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.ConnectorInfo;
import org.ballerinalang.util.codegen.FunctionInfo;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.codegen.ServiceInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.program.BLangFunctions;

import static org.ballerinalang.util.BLangConstants.INIT_FUNCTION_SUFFIX;

/**
 * Utils for accessing runtime information for Ballerina Connector SPI.
 *
 * @since 0.965.0
 */
public final class BLangConnectorSPIUtil {

    /**
     * Get ConnectorEndPoint struct.
     *
     * @param context current invocation context.
     * @return ConnectorEndPoint struct.
     */
    public static Struct getConnectorEndpointStruct(Context context) {
        BValue result = context.getControlStack().getCurrentFrame().getRefRegs()[0];
        if (result == null || result.getType().getTag() != TypeTags.STRUCT_TAG) {
            throw new BallerinaException("Can't get connector endpoint struct");
        }
        return ConnectorSPIModelHelper.createStruct((BStruct) result);
    }

    /**
     * Returns Service registered.
     *
     * Note: Call this util method when service is required, in register server connector SPI function.
     *
     * @param context invocation Context
     * @return register service.
     */
    public static Service getServiceRegisted(Context context) {
        BValue result = context.getControlStack().getCurrentFrame().getRefRegs()[1];
        if (result == null || result.getType().getTag() != TypeTags.TYPE_TAG
                || ((BTypeValue) result).value().getTag() != TypeTags.SERVICE_TAG) {
            throw new BallerinaException("Can't get service reference");
        }
        final BServiceType serviceType = (BServiceType) ((BTypeValue) result).value();
        final ProgramFile programFile = context.getProgramFile();
        final ServiceInfo serviceInfo = programFile.getPackageInfo(serviceType.getPackagePath())
                .getServiceInfo(serviceType.getName());
        final ServiceImpl service = ConnectorSPIModelHelper.createService(programFile, serviceInfo);
        Context serviceInitCtx = new Context(programFile);
        BLangFunctions.invokeFunction(programFile, serviceInfo.getInitFunctionInfo(), serviceInitCtx);
        return service;
    }

    /**
     * Creates a VM struct value.
     *
     * @param context    current context
     * @param pkgPath    package path of the struct
     * @param structName name of the struct
     * @param values     values to be copied to struct field in the defined order
     * @return created struct
     */
    public static BStruct createBStruct(Context context, String pkgPath, String structName, Object... values) {
        return createBStruct(context.getProgramFile(), pkgPath, structName, values);
    }

    public static BStruct createBStruct(ProgramFile programFile, String pkgPath, String structName, Object... values) {
        PackageInfo packageInfo = programFile.getPackageInfo(pkgPath);
        if (packageInfo == null) {
            throw new BallerinaConnectorException("package - " + pkgPath + " does not exist");
        }
        StructInfo structInfo = packageInfo.getStructInfo(structName);
        if (structInfo == null) {
            throw new BallerinaConnectorException("struct - " + structName + " does not exist");
        }
        return BLangVMStructs.createBStruct(structInfo, values);
    }

    /**
     * Wrap BVM struct value to {@link Struct}
     *
     * @param bStruct value.
     * @return wrapped value.
     */
    public static Struct toStruct(BStruct bStruct) {
        return ConnectorSPIModelHelper.createStruct(bStruct);
    }

    /**
     * Creates a VM connector value.
     *
     * @param programFile   program file
     * @param pkgPath       package path of the connector
     * @param connectorName name of the connector
     * @param args          args of the connector in the defined order
     * @return created struct
     */
    public static BConnector createBConnector(ProgramFile programFile, String pkgPath, String connectorName,
                                              Object... args) {
        PackageInfo packageInfo = programFile.getPackageInfo(pkgPath);
        if (packageInfo == null) {
            throw new BallerinaConnectorException("package - " + pkgPath + " does not exist");
        }
        ConnectorInfo connectorInfo = packageInfo.getConnectorInfo(connectorName);
        if (connectorInfo == null) {
            throw new BallerinaConnectorException("connector - " + connectorName + " does not exist");
        }
        final BConnector bConnector = BLangVMStructs.createBConnector(connectorInfo, args);
        final FunctionInfo initFunction = packageInfo.getFunctionInfo(connectorName + INIT_FUNCTION_SUFFIX);
        if (initFunction != null) {
            Context initContext = new Context(programFile);
            BLangFunctions.invokeFunction(programFile, initFunction, initContext);
        }
        return bConnector;
    }
}
